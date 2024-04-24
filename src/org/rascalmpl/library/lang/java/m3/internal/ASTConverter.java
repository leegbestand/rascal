/*******************************************************************************
 * Copyright (c) 2009-2024 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Ashim Shahi
 *   * Jurgen Vinju
 *   * Jouke Stoel
 *   * Lina María Ochoa Venegas
 *   * Davy Landman 
*******************************************************************************/
package org.rascalmpl.library.lang.java.m3.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;

/**
 * This big visitor class has a case for every type of AST node in the JDT's core DOM model.
 * Each visit method maps the JDT DOM model to the M3 AST model as defined in lang::java::m3::AST.
 * 
 * If names can be resolved, they will be, and lead to `decl=` parameters on the declarations and uses.
 * If types can be resolved, they will be interpreted as TypeSymbol's and stored on the AST nodes as `typ=` parameters.
 * All nodes get `src=` parameters pointing to their exact location in the source file.
 * 
 * Otherwise the goal of this code is to satisfy the "AST correctness" specification in analysis::m3::AST,
 * and the documentation written in lang::java::m3::AST. This means that the recovery is complete and completely
 * specific, and all node src locations point to exactly the the right input subsentences. 
 */
public class ASTConverter extends JavaToRascalConverter {
    
    public ASTConverter(final LimitedTypeStore typeStore, Map<String, ISourceLocation> cache, boolean collectBindings) {
        super(typeStore, cache, collectBindings);
    }

    @Override
    public void postVisit(ASTNode node) {
        if (getAdtType() == DATATYPE_RASCAL_AST_MODIFIER_NODE_TYPE || getAdtType() == DATATYPE_RASCAL_AST_TYPE_NODE_TYPE) {
            return;
        }
        setKeywordParameter("src", getSourceLocation(node));

        if (collectBindings) {
            ISourceLocation decl = resolveBinding(node);
            if (!decl.getScheme().equals("unknown")) {
                setKeywordParameter("decl", decl); 
            }
            if (getAdtType() != DATATYPE_RASCAL_AST_STATEMENT_NODE_TYPE) {
                IValue type = resolveType(node);

                setKeywordParameter("typ", type);
            }
        }
    }

    private IValue resolveType(ASTNode node) {
        try {
            if (node instanceof Expression) {
                if (node instanceof Name) {
                    IBinding b = ((Name) node).resolveBinding();
                    return bindingsResolver.resolveType(b, false);
                }
                ITypeBinding binding = ((Expression) node).resolveTypeBinding();
                return bindingsResolver.resolveType(binding, false);
            }
            else if (node instanceof TypeDeclaration) {
                ITypeBinding binding = ((TypeDeclaration) node).resolveBinding();
                return bindingsResolver.resolveType(binding, true);
            }
            else if (node instanceof MethodDeclaration) {
                IMethodBinding binding = ((MethodDeclaration) node).resolveBinding();
                return bindingsResolver.resolveType(binding, true);
            }
            else if (node instanceof VariableDeclaration) {
                IVariableBinding binding = ((VariableDeclaration) node).resolveBinding();
                return bindingsResolver.resolveType(binding.getType(), false);
            }
            else if (node instanceof ModuleDeclaration) {
                IModuleBinding binding = ((ModuleDeclaration) node).resolveBinding();
                return bindingsResolver.resolveType(binding, true);
            }
        } 
        catch (NullPointerException e) {
            // TODO: log this in a better way
            System.err.println("Got NPE for node " + node + ((e.getStackTrace().length > 0) ? ("\n\t" + e.getStackTrace()[0]) : ""));
        }

        // some nodes do not have a type
        return null;
    }

    
    @Override
    public void preVisit(ASTNode node) {
        // since many many nodes have annotatable possibilities, this factors
        // out the AST construction code of all of those visit methods.
        // Still every such method must put the ownAnnotations list at the right place in their constructor.

        System.err.println("Now converting: " + node.getClass().getCanonicalName() + "@ " + getSourceLocation(node));

        if (node instanceof IAnnotatable) {
            IAnnotatable annotable = (IAnnotatable) node;
            
            try {
                if (annotable.getAnnotations() == null) {
                    ownAnnotations = values.list();
                }
                else {
                    ownAnnotations = Arrays.stream(annotable.getAnnotations())
                        .map(o -> (ASTNode) o)
                        .map(a -> visitChild(a))
                        .collect(values.listWriter());
                }
            }
            catch (JavaModelException e) {
                // TODO: log this in a better way
               System.err.println("Unhandled internal error: " + e.getMessage());
            }
        }
        else {
            // This {sh,w}ould trigger downstream non-null assertions if someone tries
            // to include annotations into a non-annotable syntax node.
            // Otherwise this field should not be referenced until it is set here above again.
            ownAnnotations = null; 
        }

        // many nodes also have modifiers
        List<?> modifiers = (List<?>) node.getProperty("modifiers");
        if (modifiers != null) {
            ownModifiers = modifiers.stream()
                .map(o -> (ASTNode) o)
                .map(a -> visitChild(a))
                .collect(values.listWriter());
        }
        else {
            // This {sh,w}ould trigger downstream non-null assertions if someone tries
            // to include modifiers into a non-modifiable syntax node.
            // Otherwise this field should not be referenced until it is set here above again.
            ownModifiers = null;
        }
    }
    
    
    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);
        IValue name = visitChild(node.getName());

        IListWriter bodyDeclarations = values.listWriter();
        for (Iterator<?> it = node.bodyDeclarations().iterator(); it.hasNext();) {
            BodyDeclaration d = (BodyDeclaration) it.next();
            bodyDeclarations.append(visitChild(d));
        }

        ownValue = constructDeclarationNode("annotationType", modifiers, name, bodyDeclarations.done());
        
        return false;
    }

    @Override
    public boolean visit(AnnotationTypeMemberDeclaration node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);
        IValue typeArgument = visitChild(node.getType());

        IValue name = visitChild(node.getName());

        if (node.getDefault() != null)  {
            IValue defaultBlock = visitChild(node.getDefault());
            ownValue = constructDeclarationNode("annotationTypeMember", modifiers, typeArgument, name, defaultBlock);
        }
        else {
            ownValue = constructDeclarationNode("annotationTypeMember", modifiers, typeArgument, name);
        }
        
        return false;
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        IListWriter bodyDeclarations = values.listWriter();

        for (Iterator<?> it = node.bodyDeclarations().iterator(); it.hasNext();) {
            BodyDeclaration b = (BodyDeclaration) it.next();
            bodyDeclarations.append(visitChild(b));
        }
        ownValue = constructDeclarationNode("class", bodyDeclarations.done());

        return false;
    }

    @Override
    public boolean visit(ArrayAccess node) {
        IValue array = visitChild(node.getArray());
        IValue index = visitChild(node.getIndex());

        ownValue = constructExpressionNode("arrayAccess", array, index);

        return false;
    }

    @Override
    public boolean visit(CreationReference node) {
        IValue type = visitChild(node.getType());
        IValue typeArguments = visitChildren(node.typeArguments());

        ownValue = constructExpressionNode("creationReference", type, typeArguments);
        return false;
    }

    @Override
    public boolean visit(ArrayCreation node) {
        IValue type = visitChild(node.getType().getElementType());

        IListWriter dimensions = values.listWriter();
        for (Iterator<?> it = node.dimensions().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            dimensions.append(visitChild(e));
        }

        if (node.getInitializer() != null) {
            IValue initializer = visitChild(node.getInitializer());
            ownValue = constructExpressionNode("newArray", type, dimensions.done(), initializer);
        }
        else {
            ownValue = constructExpressionNode("newArray", type, dimensions.done()); 
        }

        return false;
    }

    @Override
    public boolean visit(ArrayInitializer node) {
        IListWriter expressions = values.listWriter();
        for (Iterator<?> it = node.expressions().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            expressions.append(visitChild(e));
        }

        ownValue = constructExpressionNode("arrayInitializer", expressions.done());

        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        int apiLevel = node.getAST().apiLevel();

        if (AST.JLS2 <= apiLevel && apiLevel <= AST.JLS4) {
            IValue type = visitChild(node.getComponentType());
            ownValue = constructTypeNode("arrayType", type);
        }
        else {
            IValue type = visitChild(node.getElementType());
            ownValue = constructTypeNode("arrayType", type);
        }

        return false;
    }

    @Override
    public boolean visit(AssertStatement node) {
        IValue expression = visitChild(node.getExpression());

        if (node.getMessage() != null) {
            IValue message = visitChild(node.getMessage());
            ownValue = constructStatementNode("assert", expression, message);
        }
        else {
            ownValue = constructStatementNode("assert", expression);
        }

        return false;
    }

    @Override
    public boolean visit(Assignment node) {
        IValue leftSide = visitChild(node.getLeftHandSide());
        IValue rightSide = visitChild(node.getRightHandSide());

        ownValue = constructExpressionNode("assignment", leftSide, values.string(node.getOperator().toString()), rightSide);

        return false;
    }

    @Override
    public boolean visit(Block node) {
        IListWriter statements = values.listWriter();
        for (Iterator<?> it = node.statements().iterator(); it.hasNext();) {
            Statement s = (Statement) it.next();
            statements.append(visitChild(s));
        }

        ownValue = constructStatementNode("block", statements.done());

        return false;
    }
    
    @Override
    public boolean visit(BlockComment node) {
        return false;
    }

    @Override
    public boolean visit(BooleanLiteral node) {
        IValue booleanValue = values.string(Boolean.toString(node.booleanValue()));

        ownValue = constructExpressionNode("booleanLiteral", booleanValue);

        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        if (node.getLabel() != null) {
            IValue label = visitChild(node.getLabel());
            ownValue = constructStatementNode("break", label);
        }
        else if (node.getExpression() != null) {
            IValue label = visitChild(node.getExpression());
            ownValue = constructStatementNode("break", label);
        }
        else {
            ownValue = constructStatementNode("break");
        }

        return false;
    }

    @Override
    public boolean visit(CastExpression node) {
        IValue type = visitChild(node.getType());
        IValue expression = visitChild(node.getExpression());

        ownValue = constructExpressionNode("cast", type, expression);

        return false;
    }

    @Override
    public boolean visit(CatchClause node) {
        IValue exception = visitChild(node.getException());
        IValue body = visitChild(node.getBody());

        ownValue = constructStatementNode("catch", exception, body);

        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        IValue value = values.string(node.getEscapedValue()); 

        ownValue = constructExpressionNode("characterLiteral", value);

        return false;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        IValue expression = node.getExpression() == null ? null : visitChild(node.getExpression());

        IValue type = null;
        IListWriter genericTypes = values.listWriter();
        if (node.getAST().apiLevel() == AST.JLS2) {
            type = visitChild(node.getName());
        } 
        else {
            type = visitChild(node.getType()); 

            if (!node.typeArguments().isEmpty()) {
                for (Iterator<?> it = node.typeArguments().iterator(); it.hasNext();) {
                    Type t = (Type) it.next();
                    genericTypes.append(visitChild(t));
                }
            }
        }

        IListWriter arguments = values.listWriter();
        for (Iterator<?> it = node.arguments().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            arguments.append(visitChild(e));
        }

        IValue anonymousClassDeclaration = node.getAnonymousClassDeclaration() == null ? null : visitChild(node.getAnonymousClassDeclaration());

        if (expression != null) {
            if (anonymousClassDeclaration != null) {
                ownValue = constructExpressionNode("newObject", expression, type, genericTypes.done(), arguments.done(), anonymousClassDeclaration);
            }
            else {
                ownValue = constructExpressionNode("newObject", expression, type, genericTypes.done(), arguments.done());
            }
        }
        else {
            if (anonymousClassDeclaration != null) {
                ownValue = constructExpressionNode("newObject", type, genericTypes.done(), arguments.done(), anonymousClassDeclaration);
            }
            else {
                ownValue = constructExpressionNode("newObject", type, genericTypes.done(), arguments.done());
            }
        }
        
        return false;
    }

    @Override
    public boolean visit(CompilationUnit node) {
        if (node.getModule() != null) {
            ownValue = constructDeclarationNode("compilationUnit", visitChild(node.getModule()));
            return false;
        }
        else {
            IValue packageOfUnit = node.getPackage() == null ? null : visitChild(node.getPackage());

            IListWriter imports = values.listWriter();
            for (Iterator<?> it = node.imports().iterator(); it.hasNext();) {
                ImportDeclaration d = (ImportDeclaration) it.next();
                imports.append(visitChild(d));
            }

            IListWriter typeDeclarations = values.listWriter();
            for (Iterator<?> it = node.types().iterator(); it.hasNext();) {
                AbstractTypeDeclaration d = (AbstractTypeDeclaration) it.next();
                typeDeclarations.append(visitChild(d));
            }

            if (packageOfUnit != null) {
                ownValue = constructDeclarationNode("compilationUnit", packageOfUnit, imports.done(), typeDeclarations.done());		
            }
            else {
                ownValue = constructDeclarationNode("compilationUnit", imports.done(), typeDeclarations.done());		
            }

            return false;
        }
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        IValue expression = visitChild(node.getExpression());
        IValue thenBranch = visitChild(node.getThenExpression());
        IValue elseBranch = visitChild(node.getElseExpression());

        ownValue = constructExpressionNode("conditional", expression, thenBranch, elseBranch);

        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        IListWriter types = values.listWriter();
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (!node.typeArguments().isEmpty()) {
                for (Iterator<?> it = node.typeArguments().iterator(); it.hasNext();) {
                    Type t = (Type) it.next();
                    types.append(visitChild(t));
                }
            }
        }

        IListWriter arguments = values.listWriter();
        for (Iterator<?> it = node.arguments().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            arguments.append(visitChild(e));
        }

        ownValue = constructStatementNode("constructorCall",  types.done(), arguments.done());

        return false;
    }

    @Override
    public boolean visit(ContinueStatement node) {
        if (node.getLabel() != null) {
            IValue label = visitChild(node.getLabel());
            ownValue = constructStatementNode("continue", label);
        }
        else {
            ownValue = constructStatementNode("continue");
        }

        return false;
    }

    @Override
    public boolean visit(DoStatement node) {
        IValue body = visitChild(node.getBody());
        IValue whileExpression = visitChild(node.getExpression());

        ownValue = constructStatementNode("do", body, whileExpression);

        return false;
    }

    @Override
    public boolean visit(EmptyStatement node) {
        ownValue = constructStatementNode("empty");

        return false;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        IValue parameter = visitChild(node.getParameter());
        IValue collectionExpression = visitChild(node.getExpression());
        IValue body = visitChild(node.getBody());

        ownValue = constructStatementNode("foreach", parameter, collectionExpression, body);

        return false;
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);
        IValue name = visitChild(node.getName());

        IListWriter arguments = values.listWriter();
        if (!node.arguments().isEmpty()) {
            for (Iterator<?> it = node.arguments().iterator(); it.hasNext();) {
                Expression e = (Expression) it.next();
                arguments.append(visitChild(e));
            }
        }

        if (node.getAnonymousClassDeclaration() != null) {
            IValue anonymousClassDeclaration = visitChild(node.getAnonymousClassDeclaration());
            ownValue = constructDeclarationNode("enumConstant", modifiers, name, arguments.done(), anonymousClassDeclaration);
        }
        else {
            ownValue = constructDeclarationNode("enumConstant", modifiers, name, arguments.done());
        }
        
        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);
        IValue name = values.string(node.getName().getFullyQualifiedName()); 

        IListWriter implementedInterfaces = values.listWriter();
        if (!node.superInterfaceTypes().isEmpty()) {
            for (Iterator<?> it = node.superInterfaceTypes().iterator(); it.hasNext();) {
                Type t = (Type) it.next();
                implementedInterfaces.append(visitChild(t));
            }
        }

        IListWriter enumConstants = values.listWriter();
        for (Iterator<?> it = node.enumConstants().iterator(); it.hasNext();) {
            EnumConstantDeclaration d = (EnumConstantDeclaration) it.next();
            enumConstants.append(visitChild(d));
        }

        IListWriter bodyDeclarations = values.listWriter();
        if (!node.bodyDeclarations().isEmpty()) {
            for (Iterator<?> it = node.bodyDeclarations().iterator(); it.hasNext();) {
                BodyDeclaration d = (BodyDeclaration) it.next();
                bodyDeclarations.append(visitChild(d));
            }
        }

        ownValue = constructDeclarationNode("enum", modifiers, name, implementedInterfaces.done(), enumConstants.done(), bodyDeclarations.done());
        
        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        IValue expression = visitChild(node.getExpression());
        ownValue = constructStatementNode("expressionStatement", expression);

        return false;
    }

    @Override
    public boolean visit(FieldAccess node) {
        IValue expression = visitChild(node.getExpression());
        IValue name = visitChild(node.getName());

        ownValue = constructExpressionNode("fieldAccess", expression, name);

        return false;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);
        IValue type = visitChild(node.getType());

        IListWriter fragments = values.listWriter();
        for (Iterator<?> it = node.fragments().iterator(); it.hasNext();) {
            VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
            fragments.append(visitChild(f));
        }

        ownValue = constructDeclarationNode("field", modifiers, type, fragments.done());
        
        return false;
    }

    @Override
    public boolean visit(ForStatement node) {
        IListWriter initializers = values.listWriter();
        for (Iterator<?> it = node.initializers().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            initializers.append(visitChild(e));
        }

        IValue booleanExpression = node.getExpression() == null ? null : visitChild(node.getExpression());

        IListWriter updaters = values.listWriter();
        for (Iterator<?> it = node.updaters().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            updaters.append(visitChild(e));
        }

        IValue body = visitChild(node.getBody());

        if (booleanExpression != null) {
            ownValue = constructStatementNode("for", initializers.done(), booleanExpression, updaters.done(), body);
        }
        else {
            ownValue = constructStatementNode("for", initializers.done(), updaters.done(), body);
        }

        return false;
    }

    @Override
    public boolean visit(IfStatement node) {
        IValue booleanExpression = visitChild(node.getExpression());
        IValue thenStatement = visitChild(node.getThenStatement());

        if (node.getElseStatement() != null) {
            IValue elseStatement = visitChild(node.getElseStatement());

            ownValue = constructStatementNode("if", booleanExpression, thenStatement, elseStatement);
        }
        else {
            ownValue = constructStatementNode("if", booleanExpression, thenStatement);
        }

        return false;
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        IValue name = visitChild(node.getName());

        IListWriter importModifiers = values.listWriter();
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (node.isStatic())
                importModifiers.append(constructModifierNode("static"));

            if (node.isOnDemand())
                importModifiers.append(constructModifierNode("onDemand"));
        }

        ownValue = constructDeclarationNode("import", name);
        setKeywordParameters("modifiers", importModifiers.done());

        return false;
    }

    @Override
    public boolean visit(InfixExpression node) {
        IValue operator = values.string(node.getOperator().toString());
        IValue leftSide = visitChild(node.getLeftOperand());
        IValue rightSide = visitChild(node.getRightOperand());

        IValue intermediateExpression = constructExpressionNode("infix", leftSide, operator, rightSide);
        for (Iterator<?> it = node.extendedOperands().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            intermediateExpression = constructExpressionNode("infix", intermediateExpression, operator, visitChild(e));
        }

        ownValue = intermediateExpression;

        return false;
    }

    @Override
    public boolean visit(Initializer node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);
        IValue body = visitChild(node.getBody());

        ownValue = constructDeclarationNode("initializer", modifiers, body);
        
        return false;
    }

    @Override
    public boolean visit(InstanceofExpression node) {
        IValue leftSide = visitChild(node.getLeftOperand());
        IValue rightSide = visitChild(node.getRightOperand());

        ownValue = constructExpressionNode("instanceof", leftSide, rightSide);

        return false;
    }

    @Override
    public boolean visit(Javadoc node) {
        return false;
    }

    @Override
    public boolean visit(LabeledStatement node) {
        IValue label = values.string(node.getLabel().getFullyQualifiedName());
        IValue body = visitChild(node.getBody());

        ownValue = constructStatementNode("label", label, body);

        return false;
    }

    @Override
    public boolean visit(LineComment node) {
        return false;
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        IValue typeName = visitChild(node.getTypeName());
        ownValue = constructExpressionNode("markerAnnotation", typeName);

        return false;
    }

    @Override
    public boolean visit(MemberRef node) {
        return false;
    }

    @Override
    public boolean visit(MemberValuePair node) {
        IValue name = values.string(node.getName().getFullyQualifiedName());
        IValue value = visitChild(node.getValue());

        ownValue = constructExpressionNode("memberValuePair", name, value);

        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        String constructorName = "method";
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);

        IListWriter genericTypes = values.listWriter();
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (!node.typeParameters().isEmpty()) {
                for (Iterator<?> it = node.typeParameters().iterator(); it.hasNext();) {
                    TypeParameter t = (TypeParameter) it.next();
                    genericTypes.append(visitChild(t));
                }
            }
        }

        IValue returnType = null;
        if (!node.isConstructor()) {
            if (node.getAST().apiLevel() == AST.JLS2) {
                returnType = visitChild(node.getReturnType());
            } else if (node.getReturnType2() != null) {
                returnType = visitChild(node.getReturnType2());
            } else {

                returnType = constructTypeNode("void");
            }
        } else {
            constructorName = "constructor";
        }

        IValue name = visitChild(node.getName());

        IListWriter parameters = values.listWriter();
        for (Iterator<?> it = node.parameters().iterator(); it.hasNext();) {
            SingleVariableDeclaration v = (SingleVariableDeclaration) it.next();
            parameters.append(visitChild(v));
        }

        IListWriter possibleExceptions = values.listWriter();

        var apiLevel = node.getAST().apiLevel();
        if (apiLevel == AST.JLS2 || apiLevel == AST.JLS3 || apiLevel == AST.JLS4) {
            if (!node.thrownExceptions().isEmpty()) {
                for (Iterator<?> it = node.thrownExceptions().iterator(); it.hasNext();) {
                    Name n = (Name) it.next();
                    possibleExceptions.append(visitChild(n));
                }
            }
        }
        else {
            if (!node.thrownExceptionTypes().isEmpty()) {
                ((List<?>) node.thrownExceptionTypes()).stream()
                    .map(o -> ((ASTNode) o))
                    .map((ASTNode n) -> visitChild(n))
                    .collect(possibleExceptions);
            }
        }

        IValue body = node.getBody() == null ? null : visitChild(node.getBody()); 
        if (body == null && constructorName.equals("constructor")) {
            body = constructStatementNode("empty"); // TODO: what about the source location of this stub?
        }

        if (body != null) {
            if ("constructor".equals(constructorName)) {
                // constructors do not have return types or additional generic types
                ownValue = constructDeclarationNode(constructorName, modifiers, name, parameters.done(), possibleExceptions.done(), body);
            }
            else {
                ownValue = constructDeclarationNode(constructorName, modifiers, genericTypes.done(), returnType, name, parameters.done(), possibleExceptions.done(), body);
            }
        }
        else {
            assert !constructorName.equals("constructor"); // constructors must have a body
            //| \method(list[Modifier] modifiers, list[Declaration] typeParameters, Type \return, Expression name, list[Declaration] parameters, list[Expression] exceptions)
            ownValue = constructDeclarationNode(constructorName, modifiers, genericTypes.done(), returnType, name, parameters.done(), possibleExceptions.done());
        }
        
        return false;
    }
    
    @Override
    public boolean visit(MethodInvocation node) {
        IListWriter genericTypes = values.listWriter();
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (!node.typeArguments().isEmpty()) {
                for (Iterator<?> it = node.typeArguments().iterator(); it.hasNext();) {
                    Type t = (Type) it.next();
                    genericTypes.append(visitChild(t));
                }
            }
        }

        IValue name = visitChild(node.getName());

        IListWriter arguments = values.listWriter();
        for (Iterator<?> it = node.arguments().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            arguments.append(visitChild(e));
            // this sometimes procudes Type instead of Expression nodes?
            if (!arguments.get(0).getType().getName().equals("Expression")) {
                System.err.println(arguments.done());
            }
        }	
        

        if (node.getExpression() != null) {
            IValue expression = visitChild(node.getExpression());
            ownValue = constructExpressionNode("methodCall", expression, genericTypes.done(), name, arguments.done());
        }
        else {
            ownValue = constructExpressionNode("methodCall", genericTypes.done(), name, arguments.done());
        }
        
        return false;
    }

    @Override
    public boolean visit(MethodRef node) {
        // Because we do not implement the JavaDoc syntax and
        // semantics, this reference to a method inside a JavaDoc comment
        // does nothing for the moment.
        return false;
    }

    @Override
    public boolean visit(MethodRefParameter node) {
        // Because we do not implement the JavaDoc syntax and
        // semantics, this reference to a parameter of a method inside a JavaDoc comment
        // does nothing for the moment.
        return false;
    }

    @Override
    public boolean visit(Modifier node) {
        String modifier = node.getKeyword().toString();
        ownValue = constructModifierNode(modifier);

        return false;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        IValue typeName = visitChild(node.getTypeName());

        IListWriter memberValuePairs = values.listWriter();
        for (Iterator<?> it = node.values().iterator(); it.hasNext();) {
            MemberValuePair p = (MemberValuePair) it.next();
            memberValuePairs.append(visitChild(p));
        }

        ownValue = constructExpressionNode("normalAnnotation", typeName, memberValuePairs.done());

        return false;
    }

    @Override
    public boolean visit(NullLiteral node) {
        ownValue = constructExpressionNode("null");

        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
        IValue number = values.string(node.getToken());

        ownValue = constructExpressionNode("number", number);

        return false;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        IValue name = visitChild(node.getName());
        IList annotations = visitChildren(node.annotations());
                        
        ownValue = constructDeclarationNode("package", annotations, name);

        return false;
    }

    @Override
    public boolean visit(ParameterizedType node) {
        IValue type = visitChild(node.getType());

        IListWriter genericTypes = values.listWriter();
        for (Iterator<?> it = node.typeArguments().iterator(); it.hasNext();) {
            Type t = (Type) it.next();
            genericTypes.append(visitChild(t));
        }

        ownValue = constructTypeNode("parameterizedType", type, genericTypes.done());
    
        return false;
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        IValue expression = visitChild(node.getExpression());
        ownValue = constructExpressionNode("bracket", expression);

        return false;
    }

    @Override
    public boolean visit(PostfixExpression node) {
        IValue operand = visitChild(node.getOperand());
        IValue operator = values.string(node.getOperator().toString());

        ownValue = constructExpressionNode("postfix", operand, operator);

        return false;
    }

    @Override
    public boolean visit(PrefixExpression node) {
        IValue operand = visitChild(node.getOperand());
        IValue operator = values.string(node.getOperator().toString());

        ownValue = constructExpressionNode("prefix", operator, operand);

        return false;
    }

    @Override
    public boolean visit(PrimitiveType node) {
        ownValue = constructTypeNode(node.toString());

        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        IValue qualifier = visitChild(node.getQualifier());

        IValue name = visitChild(node.getName());

        ownValue = constructExpressionNode("qualifiedName", qualifier, name);

        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
        IValue qualifier = visitChild(node.getQualifier());

        IValue name = visitChild(node.getName());

        ownValue = constructTypeNode("qualifiedType", qualifier, name);

        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        if (node.getExpression() != null) {
            IValue expression = visitChild(node.getExpression());
            ownValue = constructStatementNode("return", expression);
        }
        else {
            ownValue = constructStatementNode("return");
        }

        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        IValue value = values.string(node.getFullyQualifiedName());

        ownValue = constructExpressionNode("simpleName", value);

        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        IValue value = visitChild(node.getName());
        ownValue = constructTypeNode("simpleType", value);

        return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        IValue name = visitChild(node.getTypeName());
        IValue value = visitChild(node.getValue());

        ownValue = constructExpressionNode("singleMemberAnnotation", name, value);

        return false;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        IValue name = visitChild(node.getName());
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);

        IValue type = visitChild(node.getType());
        IValue initializer = node.getInitializer() == null ? null : visitChild(node.getInitializer());

        IList dimensions = ((List<?>) node.extraDimensions())
            .stream()
            .map(o -> (ASTNode) o) 
            .map(d -> visitChild(d))
            .collect(values.listWriter());

        if (node.getAST().apiLevel() >= AST.JLS3 && node.isVarargs()) {
            assert initializer == null;
            ownValue = constructDeclarationNode("vararg", modifiers, type, name);
        }
        else {
            if (initializer != null) {
                ownValue = constructDeclarationNode("parameter", modifiers, type, name, dimensions, initializer);
            }
            else {
                ownValue = constructDeclarationNode("parameter", modifiers, type, name, dimensions);
            }
        }

        return false;
    }
    
    @Override
    public boolean visit(Dimension node) {
        ownValue = constructDeclarationNode("dimension", ownAnnotations);
        return false;
    }

    @Override
    public boolean visit(StringLiteral node) {
        IString escaped = values.string(node.getEscapedValue());		
        IString literal = values.string(node.getLiteralValue());
        ownValue = constructExpressionNode("stringLiteral", escaped).asWithKeywordParameters().setParameter("literal", literal);

        return false;
    }

    @Override
    public boolean visit(TextBlock node) {
        IString escaped = values.string(node.getEscapedValue());		
        IString literal = values.string(node.getLiteralValue());
        ownValue = constructExpressionNode("textBlock", escaped).asWithKeywordParameters().setParameter("literal", literal);

        return false;
    }


    @Override
    public boolean visit(YieldStatement node) {
        IValue exp = visitChild(node.getExpression());
        ownValue = constructStatementNode("yield", exp);
        return false;
    }

    
    @Override
    public boolean visit(LambdaExpression node) {
        IListWriter parameters = values.listWriter();

        if (!node.hasParentheses()) {
            // without types for the parameters
            for (Iterator<?> it = node.parameters().iterator(); it.hasNext();) {
                VariableDeclarationFragment v = (VariableDeclarationFragment) it.next();
                parameters.append(visitChild(v));
            }
        }
        else {
            // with types for the parameters
            for (Iterator<?> it = node.parameters().iterator(); it.hasNext();) {
                VariableDeclaration v = (VariableDeclaration) it.next();
                parameters.append(constructExpressionNode("lambdaParameter", visitChild(v)));
            }
        }

        ownValue = constructExpressionNode("lambda", parameters.done(), visitChild(node.getBody()));
        return false;
    }

    @Override
    public boolean visit(SwitchExpression node) {
        IValue expression = visitChild(node.getExpression());

        IListWriter statements = values.listWriter();
        for (Iterator<?> it = node.statements().iterator(); it.hasNext();) {
            Statement s = (Statement) it.next();
            statements.append(visitChild(s));
        }

        ownValue = constructExpressionNode("switch", expression, statements.done());

        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        IValue expression = node.getExpression() == null ? null : visitChild(node.getExpression());

        IListWriter genericTypes = values.listWriter();	
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (!node.typeArguments().isEmpty()) {
                for (Iterator<?> it = node.typeArguments().iterator(); it.hasNext();) {
                    Type t = (Type) it.next();
                    genericTypes.append(visitChild(t));
                }
            }
        }

        IListWriter arguments = values.listWriter();
        for (Iterator<?> it = node.arguments().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            arguments.append(visitChild(e));
        }

        if (expression != null) {
            ownValue = constructStatementNode("superConstructorCall", expression, genericTypes.done(), arguments.done());
        }
        else {
            ownValue = constructStatementNode("superConstructorCall", genericTypes.done(), arguments.done());
        }
        
        return false;
    }

    @Override
    public boolean visit(SuperFieldAccess node) {
        IValue qualifier = node.getQualifier() == null ? null : visitChild(node.getQualifier());
        IValue name = visitChild(node.getName());

        if (qualifier != null) {
            ownValue = constructExpressionNode("fieldAccess", qualifier, name);
        }
        else {
            ownValue = constructExpressionNode("fieldAccess", name);
        }

        return false;
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        IValue qualifier = node.getQualifier() == null ? null : visitChild(node.getQualifier());

        IListWriter genericTypes = values.listWriter();
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (!node.typeArguments().isEmpty()) {
                for (Iterator<?> it = node.typeArguments().iterator(); it.hasNext();) {
                    Type t = (Type) it.next();
                    genericTypes.append(visitChild(t));
                }
            }
        }

        IValue name = visitChild(node.getName());

        IListWriter arguments = values.listWriter();
        for (Iterator<?> it = node.arguments().iterator(); it.hasNext();) {
            Expression e = (Expression) it.next();
            arguments.append(visitChild(e));
        }

        if (qualifier != null) {
            ownValue = constructExpressionNode("superMethodCall", qualifier, genericTypes.done(), name, arguments.done());
        }
        else {
            ownValue = constructExpressionNode("superMethodCall", genericTypes.done(), name, arguments.done());
        }
        
        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
        IValue expression = node.getExpression() == null ? null : visitChild(node.getExpression());
        List<?> expressions = node.expressions();

        String constructorName = "case";

        IList exprs = expression == null 
            ? values.list(expression)
            : expressions.stream()
                .map(o -> (Expression) o)
                .map(e -> visitChild(e)).collect(values.listWriter());
        
        if (node.isSwitchLabeledRule()) {
            constructorName = "caseRule";
            ownValue = constructStatementNode(constructorName, exprs);
        }
        else if (node.isDefault()) {
            constructorName = "defaultCase";
            ownValue = constructStatementNode(constructorName);
        }
        else {
            ownValue = constructStatementNode(constructorName, exprs);
        }

        return false;
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        IValue type = visitChild(node.getType());
        IList args = ((List<?>) node.typeArguments())
            .stream()
            .map(o -> (Type) o).map(t -> visitChild(t)).collect(values.listWriter());
        IValue name = visitChild(node.getName());

        ownValue = constructExpressionNode("methodReference", type, args, name);
        return false;
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        IValue type = visitChild(node.getExpression());
        IList args = ((List<?>) node.typeArguments())
            .stream()
            .map(o -> (Type) o).map(t -> visitChild(t)).collect(values.listWriter());
        IValue name = visitChild(node.getName());

        ownValue = constructExpressionNode("methodReference", type, args, name);
        return false;
    }

    @Override
    public boolean visit(ModuleDeclaration node) {
        IList mod = node.isOpen() ? values.list(constructModifierNode("open")) : values.list();
        IValue name = visitChild(node.getName());

        IList stats
            = ((List<?>) node.moduleStatements())
                .stream()
                .map(o -> (ASTNode) o)
                .map(s -> visitChild(s))
                .collect(values.listWriter());

        ownValue = constructDeclarationNode("module", mod, name, stats);
        return false;
    }

    @Override
    public boolean visit(ModuleModifier node) {
        if (node.isStatic()) {
            ownValue = constructModifierNode("static");
        }
        else if (node.isTransitive()) {
            ownValue = constructModifierNode("static");
        }
        else {
            // unknown module requirement modifier?
            assert false;
        }

        return false;
    }

    @Override
    public boolean visit(OpensDirective node) {
        IValue name = visitChild(node.getName());
        IList modules = ((List<?>) node.modules()).stream()
            .map(e -> ((ASTNode) e)) 
            .map(n -> visitChild(n))
            .collect(values.listWriter());
        
        ownValue = constructDeclarationNode("opensPackage", name, modules);
        return false;
    }

    @Override
    public boolean visit(ProvidesDirective node) {
        IValue name = visitChild(node.getName());
        IList implementations = ((List<?>) node.implementations()).stream()
            .map(e -> ((ASTNode) e)) 
            .map(n -> visitChild(n)) 
            .collect(values.listWriter());
        
        ownValue = constructDeclarationNode("providesImplementations", name, implementations);
        return false;
    }

    @Override
    public boolean visit(RequiresDirective node) {
        IList modifiers = ((List<?>) node.modifiers()).stream()
            .map(e -> ((ASTNode) e)) 
            .map(n -> visitChild(n)) 
            .collect(values.listWriter());
        IValue name = visitChild(node.getName());

        ownValue = constructDeclarationNode("requires", modifiers, name);
        return false;
    }

    @Override
    public boolean visit(UsesDirective node) {
        IValue name = visitChild(node.getName());
        
        ownValue = constructDeclarationNode("uses", name);
        return false;
    }

    @Override
    public boolean visit(ExportsDirective node) {
        IValue name = visitChild(node.getName());

        ownValue = constructDeclarationNode("exports", name);
        return false;
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        IList args = ((List<?>) node.typeArguments())
            .stream().map(o -> (Type) o)
            .map(t -> visitChild(t)).collect(values.listWriter());
        IValue name = visitChild(node.getName());

        ownValue = constructExpressionNode("superMethodReference", args, name);
        return false;
    }
        
    @Override
    public boolean visit(SwitchStatement node) {
        IValue expression = visitChild(node.getExpression());

        IListWriter statements = values.listWriter();
        for (Iterator<?> it = node.statements().iterator(); it.hasNext();) {
            Statement s = (Statement) it.next();
            statements.append(visitChild(s));
        }

        ownValue = constructStatementNode("switch", expression, statements.done());

        return false;
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        IValue expression = visitChild(node.getExpression());
        IValue body = visitChild(node.getBody());

        ownValue = constructStatementNode("synchronizedStatement", expression, body);

        return false;
    }

    @Override
    public boolean visit(TagElement node) {
        // These are recognized elements in JavaDoc code such as @see and @link.
        // We currently skip parsing those parts of the syntax so we return nothing here.
        return false;
    }

    @Override
    public boolean visit(TextElement node) {
        // These are recognized elements in JavaDoc code such as @see and @link.
        // We currently skip parsing those parts of the syntax so we return nothing here.
        return false;
    }

    @Override
    public boolean visit(ThisExpression node) {
        if (node.getQualifier() != null) {
            IValue qualifier = visitChild(node.getQualifier());

            ownValue = constructExpressionNode("this", qualifier);
        }
        else {
            ownValue = constructExpressionNode("this");
        }

        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        IValue expression = visitChild(node.getExpression());

        ownValue = constructStatementNode("throw", expression);

        return false;
    }

    @Override
    public boolean visit(TryStatement node) {
        IValue body = visitChild(node.getBody());

        IListWriter catchClauses = values.listWriter();
        for (Iterator<?> it = node.catchClauses().iterator(); it.hasNext();) {
            CatchClause cc = (CatchClause) it.next();
            catchClauses.append(visitChild(cc));
        }

        if (node.getFinally() != null) {
            IValue finallyBlock = visitChild(node.getFinally()); 

            ownValue = constructStatementNode("try", body, catchClauses.done(), finallyBlock);
        }
        else {
            ownValue = constructStatementNode("try", body, catchClauses.done());
        }

        return false;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        IList modifiers = visitChildren(node.modifiers());

        String objectType = node.isInterface() ? "interface" : "class";
        IValue name = visitChild(node.getName());

        IListWriter genericTypes = values.listWriter();
        if (node.getAST().apiLevel() >= AST.JLS3) {
            if (!node.typeParameters().isEmpty()) {			
                for (Iterator<?> it = node.typeParameters().iterator(); it.hasNext();) {
                    TypeParameter t = (TypeParameter) it.next();
                    genericTypes.append(visitChild(t));			
                }
            }
        }

        IListWriter extendsClass = values.listWriter();
        IListWriter implementsInterfaces = values.listWriter();

        if (node.getAST().apiLevel() == AST.JLS2) {
            if (node.getSuperclass() != null) {
                extendsClass.append(visitChild(node.getSuperclass()));
            }
            if (!node.superInterfaces().isEmpty()) {
                for (Iterator<?> it = node.superInterfaces().iterator(); it.hasNext();) {
                    Name n = (Name) it.next();
                    implementsInterfaces.append(visitChild(n));
                }
            }
        } else if (node.getAST().apiLevel() >= AST.JLS3) {
            if (node.getSuperclassType() != null) {
                extendsClass.append(visitChild(node.getSuperclassType()));
            }
            if (!node.superInterfaceTypes().isEmpty()) {
                for (Iterator<?> it = node.superInterfaceTypes().iterator(); it.hasNext();) {
                    Type t = (Type) it.next();
                    implementsInterfaces.append(visitChild(t));
                }
            }
        }

        IListWriter bodyDeclarations = values.listWriter();
        for (Iterator<?> it = node.bodyDeclarations().iterator(); it.hasNext();) {
            BodyDeclaration d = (BodyDeclaration) it.next();
            bodyDeclarations.append(visitChild(d));
        }

        ownValue = constructDeclarationNode(objectType, modifiers, name, genericTypes.done(), extendsClass.done(), implementsInterfaces.done(), bodyDeclarations.done());
        
        return false;
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        IValue typeDeclaration;

        if (node.getAST().apiLevel() == AST.JLS2) {
            typeDeclaration = visitChild(node.getTypeDeclaration());
        }
        else {
            typeDeclaration = visitChild(node.getDeclaration());
        }

        ownValue = constructStatementNode("declarationStatement", typeDeclaration);

        return false;
    }

    @Override
    public boolean visit(TypeLiteral node) {
        IValue type = visitChild(node.getType());

        ownValue = constructExpressionNode("type", type);

        return false;
    }

    @Override
    public boolean visit(TypeParameter node) {
        IValue name = visitChild(node.getName());

        IListWriter extendsList = values.listWriter();
        if (!node.typeBounds().isEmpty()) {
            for (Iterator<?> it = node.typeBounds().iterator(); it.hasNext();) {
                Type t = (Type) it.next();
                extendsList.append(visitChild(t));
            }
        }

        ownValue = constructDeclarationNode("typeParameter", name, extendsList.done());

        return false;
    }

    @Override
    public boolean visit(UnionType node) {
        IListWriter typesValues = values.listWriter();
        for(Iterator<?> types = node.types().iterator(); types.hasNext();) {
            Type type = (Type) types.next();
            typesValues.append(visitChild(type));
        }

        ownValue = constructTypeNode("unionType", typesValues.done());

        return false;
    }

    @Override
    public boolean visit(IntersectionType node) {
        IListWriter typesValues = values.listWriter();
        for(Iterator<?> types = node.types().iterator(); types.hasNext();) {
            Type type = (Type) types.next();
            typesValues.append(visitChild(type));
        }

        ownValue = constructTypeNode("intersectionType", typesValues.done());

        return false;
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);

        IValue type = visitChild(node.getType());

        IListWriter fragments = values.listWriter();
        for (Iterator<?> it = node.fragments().iterator(); it.hasNext();) {
            VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
            fragments.append(visitChild(f));
        }

        // intented nesting; we're reusing the Declaration AST node here.
        ownValue = constructDeclarationNode("variables", modifiers, type, fragments.done());
        ownValue = constructExpressionNode("declarationExpression", ownValue);

        return false;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        IValue name = visitChild(node.getName());

        IList dimensions = ((List<?>) node.extraDimensions())
            .stream()
            .map(o -> (ASTNode) o) 
            .map(d -> visitChild(d))
            .collect(values.listWriter());

        if (node.getInitializer() != null) {
            IValue initializer = visitChild(node.getInitializer());
            ownValue = constructExpressionNode("variable", name, dimensions, initializer);
        }
        else {
            ownValue = constructExpressionNode("variable", name, dimensions);
        }


        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        IList modifiers = mergeModifiersAndAnnotationsInOrderOfAppearance(ownModifiers, ownAnnotations);

        IValue type = visitChild(node.getType());

        IListWriter fragments = values.listWriter();
        for (Iterator<?> it = node.fragments().iterator(); it.hasNext();) {
            VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
            fragments.append(visitChild(f));
        }

        // intented nesting; we reuse the declaration node inside a statement node
        ownValue = constructDeclarationNode("variables", modifiers, type, fragments.done());
        ownValue = constructStatementNode("declarationStatement", ownValue);

        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        IValue expression = visitChild(node.getExpression());
        IValue body = visitChild(node.getBody());

        ownValue = constructStatementNode("while", expression, body);

        return false;
    }

    @Override
    public boolean visit(WildcardType node) {
        String name = "wildcard";
        IList annos = visitChildren(node.annotations());

        if (node.getBound() != null) {
            var bound = visitChild(node.getBound());
            if (node.isUpperBound()) {
                name = "extends";
            } 
            else {
                name = "super";
            }    

            ownValue = constructTypeNode(name, annos, bound);
        }
        else {
            ownValue = constructTypeNode(name, annos);
        }

        return false;
    }

}
