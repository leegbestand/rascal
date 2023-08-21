module lang::rascal::tests::concrete::Patterns5


syntax ConcreteHole = ;

import ParseTree;
anno loc Tree@src;

// A concrete pattern for EXP with two EXP holes

Tree expression =
appl(prod(label("concrete",sort("Pattern")),[label("concrete",lex("Concrete"))],{}),[appl(prod(label("parsed",lex("Concrete")),[sort("EXP")],{}),[appl(prod(label("addition",sort("EXP")),[label("lhs",sort("EXP")),layouts("LAYOUTLIST"),lit("+"),layouts("LAYOUTLIST"),label("rhs",conditional(sort("EXP"),{except("match"),except("noMatch")}))],{\assoc(\Associativity::\left())}),[appl(prod(label("$MetaHole",sort("EXP")),[sort("ConcreteHole")],{Attr::\tag("holeType"(sort("EXP")))}),[appl(prod(label("one",sort("ConcreteHole")),[lit("\<"),layouts("LAYOUTLIST"),label("symbol",sort("Sym")),layouts("LAYOUTLIST"),label("name",lex("Name")),layouts("LAYOUTLIST"),lit("\>")],{}),[appl(prod(lit("\<"),[\char-class([range(60,60)])],{}),[char(60)]),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(142,0,<6,35>,<6,35>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(142,0,<6,35>,<6,35>)),appl(prod(label("nonterminal",sort("Sym")),[conditional(label("nonterminal",lex("Nonterminal")),{\not-follow(lit("["))})],{}),[appl(prod(lex("Nonterminal"),[conditional(seq([conditional(\char-class([range(65,90)]),{\not-precede(\char-class([range(65,90)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})]),{delete(keywords("RascalKeywords"))})],{}),[appl(regular(seq([conditional(\char-class([range(65,90)]),{\not-precede(\char-class([range(65,90)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})])),[char(69),appl(regular(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))),[char(120),char(112),char(114),char(101),char(115),char(115),char(105),char(111),char(110)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(143,9,<6,36>,<6,45>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(142,10,<6,35>,<6,45>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(142,10,<6,35>,<6,45>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(142,10,<6,35>,<6,45>)),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[appl(prod(lex("LAYOUT"),[\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])],{}),[char(32)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(152,1,<6,45>,<6,46>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(152,1,<6,45>,<6,46>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(152,1,<6,45>,<6,46>)),appl(prod(lex("Name"),[conditional(seq([conditional(\char-class([range(65,90),range(95,95),range(97,122)]),{\not-precede(\char-class([range(65,90),range(95,95),range(97,122)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})]),{delete(keywords("RascalKeywords"))})],{}),[appl(regular(seq([conditional(\char-class([range(65,90),range(95,95),range(97,122)]),{\not-precede(\char-class([range(65,90),range(95,95),range(97,122)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})])),[char(95),appl(regular(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))),[],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(154,0,<6,47>,<6,47>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(153,1,<6,46>,<6,47>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(153,1,<6,46>,<6,47>)),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(154,0,<6,47>,<6,47>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(154,0,<6,47>,<6,47>)),appl(prod(lit("\>"),[\char-class([range(62,62)])],{}),[char(62)])],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(141,14,<6,34>,<6,48>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(141,14,<6,34>,<6,48>)),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[appl(prod(lex("LAYOUT"),[\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])],{}),[char(32)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(155,1,<6,48>,<6,49>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(155,1,<6,48>,<6,49>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(155,1,<6,48>,<6,49>)),appl(prod(lit("+"),[\char-class([range(43,43)])],{}),[char(43)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(156,1,<6,49>,<6,50>)),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[appl(prod(lex("LAYOUT"),[\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])],{}),[char(32)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(157,1,<6,50>,<6,51>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(157,1,<6,50>,<6,51>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(157,1,<6,50>,<6,51>)),appl(prod(label("$MetaHole",sort("EXP")),[sort("ConcreteHole")],{Attr::\tag("holeType"(sort("EXP")))}),[appl(prod(label("one",sort("ConcreteHole")),[lit("\<"),layouts("LAYOUTLIST"),label("symbol",sort("Sym")),layouts("LAYOUTLIST"),label("name",lex("Name")),layouts("LAYOUTLIST"),lit("\>")],{}),[appl(prod(lit("\<"),[\char-class([range(60,60)])],{}),[char(60)]),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(159,0,<6,52>,<6,52>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(159,0,<6,52>,<6,52>)),appl(prod(label("nonterminal",sort("Sym")),[conditional(label("nonterminal",lex("Nonterminal")),{\not-follow(lit("["))})],{}),[appl(prod(lex("Nonterminal"),[conditional(seq([conditional(\char-class([range(65,90)]),{\not-precede(\char-class([range(65,90)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})]),{delete(keywords("RascalKeywords"))})],{}),[appl(regular(seq([conditional(\char-class([range(65,90)]),{\not-precede(\char-class([range(65,90)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})])),[char(69),appl(regular(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))),[char(120),char(112),char(114),char(101),char(115),char(115),char(105),char(111),char(110)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(160,9,<6,53>,<6,62>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(159,10,<6,52>,<6,62>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(159,10,<6,52>,<6,62>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(159,10,<6,52>,<6,62>)),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[appl(prod(lex("LAYOUT"),[\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])],{}),[char(32)],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(169,1,<6,62>,<6,63>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(169,1,<6,62>,<6,63>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(169,1,<6,62>,<6,63>)),appl(prod(lex("Name"),[conditional(seq([conditional(\char-class([range(65,90),range(95,95),range(97,122)]),{\not-precede(\char-class([range(65,90),range(95,95),range(97,122)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})]),{delete(keywords("RascalKeywords"))})],{}),[appl(regular(seq([conditional(\char-class([range(65,90),range(95,95),range(97,122)]),{\not-precede(\char-class([range(65,90),range(95,95),range(97,122)]))}),conditional(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)])),{\not-follow(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))})])),[char(95),appl(regular(\iter-star(\char-class([range(48,57),range(65,90),range(95,95),range(97,122)]))),[],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(171,0,<6,64>,<6,64>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(170,1,<6,63>,<6,64>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(170,1,<6,63>,<6,64>)),appl(prod(layouts("LAYOUTLIST"),[conditional(\iter-star(lex("LAYOUT")),{\not-follow(\char-class([range(9,13),range(32,32),range(133,133),range(160,160),range(5760,5760),range(6158,6158),range(8192,8202),range(8232,8233),range(8239,8239),range(8287,8287),range(12288,12288)])),\not-follow(lit("//")),\not-follow(lit("/*"))})],{}),[appl(regular(\iter-star(lex("LAYOUT"))),[],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(171,0,<6,64>,<6,64>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(171,0,<6,64>,<6,64>)),appl(prod(lit("\>"),[\char-class([range(62,62)])],{}),[char(62)])],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(158,14,<6,51>,<6,65>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(158,14,<6,51>,<6,65>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(141,31,<6,34>,<6,65>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(127,46,<6,20>,<6,66>))],src=|project://rascal-core/src/org/rascalmpl/core/library/lang/rascalcore/compile/Examples/Tst0.rsc|(127,46,<6,20>,<6,66>))
;


list[Symbol] getHoleTypes(appl(prod(label("concrete",sort("Pattern")),[label("concrete",lex("Concrete"))], {}),[Tree concrete1])){
    list[Symbol] holes = [];
    if(appl(prod(Symbol::label("parsed",Symbol::lex("Concrete")), [_],_),[Tree concrete2]) := concrete1){
        for(/appl(prod(Symbol::label("$MetaHole", Symbol _),[Symbol::sort("ConcreteHole")], {\tag("holeType"(Symbol holeType))}), [ConcreteHole _]) := concrete2){
            holes += holeType;
        }
    }
    return holes;
} 

test bool getHoleTypesInConcretePattern() = getHoleTypes(expression) == [ sort("EXP"), sort("EXP") ];