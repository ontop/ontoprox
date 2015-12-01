:- dynamic fresh/1.

%%%%%%%%%% various utility predicates for lists and sequences %%%%%%%%%%

flatten(T, [T]) :- (var(T); atomic(T)), !.
flatten((T1, T2), L) :- flatten(T1, L1), flatten(T2, L2), append(L1, L2, L), !.
flatten(T, [T]) :- compound(T), !.


to_sequence([T], T) :- !.
to_sequence([H|T], (H, TS)) :- to_sequence(T, TS).


member_seq(X, (Y,_)) :- X==Y, !.
member_seq(X, Y) :- X==Y, !.
member_seq(X,(_,T)) :- member_seq(X,T).

%member(X, [X|_]) :- !.
%member(X,[_|T]) :- member(X,T).

%% removes duplicates
remove_unifiable([],[]).
remove_unifiable([H|T],[H|Out]) :-
    not(member(H,T)), !, %%% check here
    remove_unifiable(T,Out).
remove_unifiable([H|T],Out) :-
    remove_unifiable(T,Out).


%% checks whether the exact syntactic form of Item appears in List  
%% member_list(Item, List) 
member_list(X, [Y|_]) :- X==Y, !.
member_list(X,[_|T]) :- member_list(X,T).

%% removes the exact multiple syntactic occurrences from a list
remove_duplicates([],[]).
remove_duplicates([H|T],[H|Out]) :-
    not(member_list(H,T)), !, %%% check here
    remove_duplicates(T,Out).
remove_duplicates([H|T],Out) :-
    remove_duplicates(T,Out).


%% checks whether a variation (=@=) of Item appears in List  
%% member_list(Item, List) 
memberEq(X,[Y|_]) :- X =@= Y, !.
memberEq(X,[_|T]) :- memberEq(X,T).

%% removes occurrences of items for which there is an 
%% "equivalent" (=@=) item in a list
remove_equivalent([],[]).
remove_equivalent([H|T],[H|Out]) :-
    not(memberEq(H,T)), !,  %%% check here
    remove_equivalent(T,Out).
remove_equivalent([H|T],Out) :-
%    memberEq(H,T),
    remove_equivalent(T,Out).


:- redefine_system_predicate(print(_)).
print([H | T]) :- write_term(H, []), nl, print(T).
print([]) :- nl.  



%%%%%%%%%% Predicates that implement the semantics of Description Logics %%%%%%%%%% 
%%%%%%%%%% used in optimizing the expansions %%%%%%%%%%


%% using assert
%
%:- dynamic table_subsumptions/2.
%
%rdfs_subsumes(A,B) :- table_subsumptions(A,B), !.
%rdfs_subsumes(A,B) :-
%  t_rdfs_subsumes(A, B, [A]),
%  asserta(table_subsumptions(A,B)).                   
%
%rdfs_subsumes_0(A,B) :- table_subsumptions(A,B), !.
%rdfs_subsumes_0(A,B) :-
%  clause(A, B), not(B = (_,_)), not(edb(B)).
%  asserta(table_subsumptions(A,B)).                   

rdfs_subsumes(A, B) :-
	t_rdfs_subsumes(A, B, [A]).                   

rdfs_subsumes_0(A,B) :-
	clause(A, B), not(B = (_,_)), not(edb(B)).

t_rdfs_subsumes(A, B, L) :- 
	rdfs_subsumes_0(A,B), not(member(B, L)).

t_rdfs_subsumes(A, B, IntermediateNodes) :-     
	rdfs_subsumes_0(A,C), not(member(C, IntermediateNodes)),
	t_rdfs_subsumes(C, B, [C | IntermediateNodes]).


subsumptions(A,B) :- idb(A), rdfs_subsumes(A,B).


remove_subsumers_1(_, [], []).

remove_subsumers_1(OriginalList, [Current|ListToProcess], Out) :-
	delete(OriginalList, Current, WithoutCurrent), 
	subsumes_from_list(Current, WithoutCurrent), !, 
	remove_subsumers_1(OriginalList, ListToProcess, Out).

remove_subsumers_1(OriginalList, [Current|ListToProcess], [Current|Out]) :-
	remove_subsumers_1(OriginalList, ListToProcess, Out).

%% removes from L predicates that are super classes or super roles of other predicates in the list
remove_subsumers(L,Out) :- 
	remove_duplicates(L, WithoutDuplicates),
	remove_subsumers_1(WithoutDuplicates, WithoutDuplicates, Out).


subsumes_from_list(_, []) :- fail.
subsumes_from_list(Elem, [H|_]) :- Elem = view(Sup), H = view(Sub), rdfs_subsumes(Sup, Sub), !.  
subsumes_from_list(Elem, [H|_]) :- rdfs_subsumes(Elem, H), !.  
subsumes_from_list(Elem, [_|T]) :- subsumes_from_list(Elem, T).  



%%%%%%%%%% Computation of Datalog Expasions %%%%%%%%%%

expand(call(_), _, _) :- !, fail. % critical !!!

expand(Goal, Goal, _) :- edb(Goal), !. % EDB predicate should not be expanded. They will be defined by the mappings

expand(Goal, Goal, 0) :- !, fail. % this means you reach an IDB predicate, which should not be expanded further

%expand((Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
%    Depth >= 1, Depth_1 is Depth - 1,  expand(Goal1, Expansion1, Depth_1), expand(Goal2, Expansion2, Depth_1).

expand((Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
    expand(Goal1, Expansion1, Depth), 
    expand(Goal2, Expansion2, Depth).

expand(Goal, Expansion, Depth) :-
    clause(Goal, Body),  Depth >= 1, Depth_1 is Depth - 1, 
    expand(Body, Expansion, Depth_1).


optimized_expand(_, call(_), _, _) :- !, fail. % critical !!!

optimized_expand(_, Goal, Goal, _) :- edb(Goal), !. % EDB predicate should not be expanded. They will be defined by the mappings

optimized_expand(_, Goal, Goal, 0) :- !, fail. % this means you reach an IDB predicate, which should not be expanded further

optimized_expand(OriginalGoal, (Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
    optimized_expand(OriginalGoal, Goal1, Expansion1, Depth), 
    optimized_expand(OriginalGoal, Goal2, Expansion2, Depth).

optimized_expand(OriginalGoal, Goal, Expansion, Depth) :-
    clause(Goal, Body),  not(member_seq(OriginalGoal, Body)), Depth >= 1, Depth_1 is Depth - 1, 
    optimized_expand(OriginalGoal, Body, Expansion, Depth_1).



subsumption_optimized_expand_0(_, call(_), _, _) :- !, fail. % critical !!!

subsumption_optimized_expand_0(_, Goal, [Goal], _) :- edb(Goal), !. % EDB predicate should not be expanded. They will be defined by the mappings

subsumption_optimized_expand_0(_, Goal, [Goal], 0) :- !, fail. % this means you reach an IDB predicate, which should not be expanded further

subsumption_optimized_expand_0(OriginalGoal, Goal, Expansion, Depth) :-
    expand_0(OriginalGoal, Goal, Body),
    %% 
    %% expand first the fresh predicates and then remove subsumers 
    %%
    expand_fresh(OriginalGoal, Body, ExpansionOfFresh), 
    remove_subsumers(ExpansionOfFresh, Optimized), %print([OptBodySequence]),nl,
	%%
	%% the recursive call
    %%
    Depth >= 1, Depth_1 is Depth - 1,
    subsumption_optimized_expand(OriginalGoal, Optimized, Expansion, Depth_1).

subsumption_optimized_expand(_, [], [], _) :- !. 

subsumption_optimized_expand(OriginalGoal, [Goal1|Goal2], Expansions, Depth) :- 
    subsumption_optimized_expand_0(OriginalGoal, Goal1, Expansion1, Depth), 
    subsumption_optimized_expand(OriginalGoal, Goal2, Expansion2, Depth),
	append(Expansion1, Expansion2, OrigExps), remove_subsumers(OrigExps, Expansions).


expand_fresh(_, [], []) :- !.

expand_fresh(OriginalGoal, [Goal1|Goal2], Expansion) :- 
	fresh(Goal1), expand_0(OriginalGoal, Goal1, Body1), 
	expand_fresh(OriginalGoal, Goal2, Expansion2), 
	append(Body1, Expansion2, Expansion), !.    

expand_fresh(OriginalGoal, [Goal1|Goal2], [Goal1|Expansion2]) :- 
	expand_fresh(OriginalGoal, Goal2, Expansion2).    


%% the basic expand
%% - gets a body of goal
%% - flattens it to a list
%% - checks that the original goal is not the list <-- we bring this check down  ??? TODO check if it's really an optimization
expand_0(OriginalGoal, Goal, Expansion) :-
    clause(Goal, Body), flatten(Body, Expansion), not(member_list(OriginalGoal, Expansion)).




%%%%%%%%%%%%% breadth first expand %%%%%%%%%%%%%
%% slower than the normal one

optimized_bf_expand(_, Goal, Goal, _) :-
	edb_expansions(Goal), !.

optimized_bf_expand(OriginalGoal, Goal, Expansions, Depth) :-
    Depth >= 1, Depth_1 is Depth - 1,
	expand_one_level(OriginalGoal, Goal, FirstLevelExpansion), 
	expand_fresh(OriginalGoal, FirstLevelExpansion, ExpansionOfFresh), 
    remove_subsumers(ExpansionOfFresh, Optimized), 
	optimized_bf_expand(OriginalGoal, Optimized, Expansions, Depth_1).

edb_expansions([]) :- !.
edb_expansions([H|T]) :- edb(H), edb_expansions(T). 

bf_expand_0(_, call(_), _) :-
    !, fail. 			%% check here
bf_expand_0(_, Goal, [Goal]) :-
    edb(Goal), !. 		%% check here
bf_expand_0(OriginalGoal, Goal, Expansion) :-
    clause(Goal, Body), flatten(Body, Expansion), not(member_list(OriginalGoal, Expansion)).

expand_one_level(_, [], []) :- !.
expand_one_level(OriginalGoal, [Goal1|Goal2], Expansion) :- 
	bf_expand_0(OriginalGoal, Goal1, Body1), 
	expand_one_level(OriginalGoal, Goal2, Expansion2), 
	append(Body1, Expansion2, Expansion).    

%%%%%%%%%%%%% end of breadth first expand %%%%%%%%%%%%%



%expand_list(Goal, GoalAndExpansion, Depth) :- expand(Goal, Expansion, Depth), 
%    flatten(Expansion, ExpansionList), GoalAndExpansion = (Goal, ExpansionList).
expand_list(Goal, GoalAndExpansion, Depth) :- optimized_expand(Goal, Goal, Expansion, Depth),
    flatten(Expansion, ExpansionList), GoalAndExpansion = (Goal, ExpansionList).

expand_list_opt(Goal, GoalAndExpansion, Depth) :- 
	subsumption_optimized_expand(Goal, [Goal], Expansion, Depth),
    GoalAndExpansion = (Goal, Expansion).

%% breadth first
expand_list_bf(Goal, GoalAndExpansion, Depth) :- 
	optimized_bf_expand(Goal, [Goal], Expansion, Depth), 
	edb_expansions(Expansion),
    GoalAndExpansion = (Goal, Expansion).

datalog_expansions(P, Depth, Expansions) :-
    findall(Expansion, expand_list(P, Expansion, Depth), IntermediateExpansions),
    remove_equivalent(IntermediateExpansions, Expansions).

datalog_expansions_opt(P, Depth, Expansions) :-
    findall(Expansion, expand_list_opt(P, Expansion, Depth), IntermediateExpansions),
    remove_equivalent(IntermediateExpansions, Expansions).

datalog_expansions_bf(P, Depth, Expansions) :-
    findall(Expansion, expand_list_bf(P, Expansion, Depth), IntermediateExpansions),
    remove_equivalent(IntermediateExpansions, Expansions).



:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#AssistantProfessor'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#AssociateProfessor'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Book'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ClericalStaff'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#College'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ConferencePaper'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#FullProfessor'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateStudent'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#JournalArticle'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Lecturer'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Manual'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchAssistant'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchGroup'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Specification'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SystemsStaff'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#TechnicalReport'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#UndergraduateStudent'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#UnofficialPublication'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Woman'/1.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#emailAddress'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#enrollIn'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#firstName'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMajor'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAdvisedBy'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isCrazyAbout'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#lastName'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#like'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#name'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationAuthor'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#researchInterest'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#takesCourse'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#telephone'/2.
:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'/2.

edb(view(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#AssistantProfessor'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#AssociateProfessor'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Book'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#ClericalStaff'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#College'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#ConferencePaper'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#FullProfessor'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateStudent'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#JournalArticle'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Lecturer'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Manual'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchAssistant'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchGroup'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Specification'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#SystemsStaff'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#TechnicalReport'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#UndergraduateStudent'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#UnofficialPublication'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#Woman'(_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#emailAddress'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#enrollIn'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#firstName'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMajor'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#isAdvisedBy'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#isCrazyAbout'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#lastName'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#name'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationAuthor'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#researchInterest'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#takesCourse'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#telephone'(_,_)).
idb('http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'(_,_)).

'http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AssistantProfessor'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#AssistantProfessor'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AssociateProfessor'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#AssociateProfessor'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Book'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Book'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#ClericalStaff'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#ClericalStaff'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#College'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#College'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#ConferencePaper'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#ConferencePaper'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#FullProfessor'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#FullProfessor'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateStudent'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateStudent'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#JournalArticle'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#JournalArticle'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Lecturer'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Lecturer'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Manual'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Manual'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchAssistant'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchAssistant'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchGroup'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchGroup'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Specification'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Specification'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#SystemsStaff'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#SystemsStaff'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#TechnicalReport'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#TechnicalReport'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#UndergraduateStudent'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#UndergraduateStudent'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#UnofficialPublication'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#UnofficialPublication'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Woman'(X) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#Woman'(X)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#emailAddress'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#emailAddress'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#enrollIn'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#enrollIn'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#firstName'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#firstName'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMajor'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMajor'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAdvisedBy'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#isAdvisedBy'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isCrazyAbout'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#isCrazyAbout'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#lastName'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#lastName'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#name'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#name'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationAuthor'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationAuthor'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#researchInterest'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#researchInterest'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#takesCourse'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#takesCourse'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#telephone'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#telephone'(X, Y)).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'(X, Y) :- view('http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'(X, Y)).
fresh('http://www.example.org/fresh#isStudentOf_some_Organization'(_)).
fresh('http://www.example.org/fresh#teachingAssistantOf_some_Course'(_)).
fresh('http://www.example.org/fresh#isHeadOf_some_Program'(_)).
fresh('http://www.example.org/fresh#worksFor_some_Organization'(_)).
fresh('http://www.example.org/fresh#like_some_Sports'(_)).
fresh('http://www.example.org/fresh#isHeadOf_some_College'(_)).
fresh('http://www.example.org/fresh#isHeadOf_some_Department'(_)).
fresh('http://www.example.org/fresh#isCrazyAbout_some_Sports'(_)).
fresh('http://www.example.org/fresh#hasMajor_some_Science'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_0'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_1'(_)).
fresh('http://www.example.org/fresh#not_Man'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_3'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_2'(_)).
fresh('http://www.example.org/fresh#Person_and_worksFor_some_Organization'(_)).
fresh('http://www.example.org/fresh#Person_and_isCrazyAbout_some_Sports'(_)).
fresh('http://www.example.org/fresh#Person_and_teachingAssistantOf_some_Course'(_)).
fresh('http://www.example.org/fresh#Person_and_like_some_Sports'(_)).
fresh('http://www.example.org/fresh#Person_and_isStudentOf_some_Organization'(_)).
fresh('http://www.example.org/fresh#Person_and_isHeadOf_some_Program'(_)).
fresh('http://www.example.org/fresh#Student_and_hasMajor_some_Science'(_)).
fresh('http://www.example.org/fresh#Person_and_isHeadOf_some_Department'(_)).


'http://uob.iodt.ibm.com/univ-bench-dl.owl#ScienceStudent'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X),'http://www.example.org/fresh#hasMajor_some_Science'(X).
'http://www.example.org/fresh#isStudentOf_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Faculty'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#PostDoc'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Insterest'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Music'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Insterest'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Sports'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ConferencePaper'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#JournalArticle'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Dean'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#College'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#WomanCollege'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#SupportingStaff'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SystemsStaff'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsFan'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#isCrazyAbout_some_Sports'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateCourse'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsLover'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Work'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Research'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchGroup'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#AssociateProfessor'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'(X).
'http://www.example.org/fresh#teachingAssistantOf_some_Course'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Manual'(X).
'http://www.example.org/fresh#isHeadOf_some_Program'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Director'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#NonScienceStudent'(X),'http://www.example.org/fresh#hasMajor_some_Science'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.example.org/fresh#eliminateMinCard_fresh4'(X),'http://www.example.org/fresh#eliminateMinCard_fresh3'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Employee'(X) :- 'http://www.example.org/fresh#worksFor_some_Organization'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#NonScienceStudent'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchAssistant'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#FullProfessor'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Employee'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SupportingStaff'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'(X),'http://www.example.org/fresh#not_Man'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Dean'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_0'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Faculty'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X).
'http://www.example.org/fresh#worksFor_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Director'(X).
'http://www.example.org/fresh#like_some_Sports'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsLover'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AcademicSubject'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Engineering'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#UnofficialPublication'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Institute'(X).
'http://www.example.org/fresh#isHeadOf_some_College'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Dean'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Book'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.example.org/fresh#eliminateMinCard_fresh3'(X),'http://www.example.org/fresh#eliminateMinCard_fresh5'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#VisitingProfessor'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_1'(X).
'http://www.example.org/fresh#isHeadOf_some_Department'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X).
'http://www.example.org/fresh#worksFor_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Dean'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Employee'(X).
'http://www.example.org/fresh#worksFor_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#isStudentOf_some_Organization'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Work'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Faculty'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Lecturer'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsLover'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#like_some_Sports'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#UndergraduateStudent'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Program'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateStudent'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsFan'(X).
'http://www.example.org/fresh#worksFor_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchAssistant'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#College'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Specification'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AcademicSubject'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#FineArts'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_2'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_3'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Director'(X) :- 'http://www.example.org/fresh#isHeadOf_some_Program'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#SupportingStaff'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ClericalStaff'(X).
'http://www.example.org/fresh#like_some_Sports'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsFan'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AcademicSubject'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#HumanitiesAndSocial'(X).
'http://www.example.org/fresh#isCrazyAbout_some_Sports'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#SportsFan'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Dean'(X) :- 'http://www.example.org/fresh#isHeadOf_some_College'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.example.org/fresh#eliminateMinCard_fresh4'(X),'http://www.example.org/fresh#eliminateMinCard_fresh5'(X).
'http://www.example.org/fresh#hasMajor_some_Science'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ScienceStudent'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Article'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#TechnicalReport'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#AssistantProfessor'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#ScienceStudent'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#PeopleWithHobby'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Woman'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#Man'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AcademicSubject'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Science'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Director'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(X) :- 'http://www.example.org/fresh#teachingAssistantOf_some_Course'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X).
'http://www.example.org/fresh#worksFor_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Employee'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'(X) :- 'http://www.example.org/fresh#isHeadOf_some_Department'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Woman'(X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Employee'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Faculty'(X).
'http://www.example.org/fresh#_eliminatedtransfresh_0'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#softwareDocumentation'(X,Y).
'http://www.example.org/fresh#hasMajor_some_Science'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Science'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMajor'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#takesCourse'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Faculty'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teacherOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasAlumnus'(Y,X).
'http://www.example.org/fresh#like_some_Sports'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Sports'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationResearch'(Y,X).
'http://www.example.org/fresh#isCrazyAbout_some_Sports'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Sports'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#isCrazyAbout'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateCourse'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#GraduateStudent'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#takesCourse'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'(Y,X).
'http://www.example.org/fresh#teachingAssistantOf_some_Course'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(Y,X).
'http://www.example.org/fresh#isHeadOf_some_Program'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Program'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#softwareDocumentation'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Research'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationResearch'(X,Y).
'http://www.example.org/fresh#_eliminatedtransfresh_1'(Y) :- 'http://www.example.org/fresh#_eliminatedtransfresh_1'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#AcademicSubject'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMajor'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAffiliateOf'(X,Y).
'http://www.example.org/fresh#not_Man'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#WomanCollege'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAdvisedBy'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_1'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAffiliatedOrganizationOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Faculty'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#orgPublication'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAffiliateOf'(Y,X).
'http://www.example.org/fresh#isHeadOf_some_Department'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#orgPublication'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(X,Y).
'http://www.example.org/fresh#_eliminatedtransfresh_3'(Y) :- 'http://www.example.org/fresh#_eliminatedtransfresh_3'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#TeachingAssistant'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teachingAssistantOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasAlumnus'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationAuthor'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Software'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#softwareVersion'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAdvisedBy'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Professor'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#tenured'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teacherOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#ResearchGroup'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#researchProject'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Research'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#researchProject'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#UndergraduateStudent'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#WomanCollege'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(X,Y).
'http://www.example.org/fresh#isStudentOf_some_Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_2'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(Y,X).
'http://www.example.org/fresh#isHeadOf_some_College'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#College'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationDate'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_3'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMember'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Department'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#enrollIn'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'(Y,X).
'http://www.example.org/fresh#worksFor_some_Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isAffiliatedOrganizationOf'(X,Y).
'http://www.example.org/fresh#_eliminatedtransfresh_0'(Y) :- 'http://www.example.org/fresh#_eliminatedtransfresh_0'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#University'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(X,Y).
'http://www.example.org/fresh#_eliminatedtransfresh_2'(Y) :- 'http://www.example.org/fresh#_eliminatedtransfresh_2'(X),'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Publication'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#publicationAuthor'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Organization'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMember'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Schedule'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#listedCourse'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#Course'(Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#listedCourse'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#love'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasUndergraduateDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMember'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMasterDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMember'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isCrazyAbout'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teacherOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMember'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#teacherOf'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#worksFor'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isHeadOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasAlumnus'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#love'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#like'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDoctoralDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isMemberOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasMember'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasAlumnus'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#teacherOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasAlumnus'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#teacherOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#enrollIn'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isFriendOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasAlumnus'(X,Y) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasDegreeFrom'(Y,X).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasStudent'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isStudentOf'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(Y,X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(X,Y).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(X,Z) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(X,Y),'http://uob.iodt.ibm.com/univ-bench-dl.owl#hasSameHomeTownWith'(Y,Z).
'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(X,Z) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(X,Y),'http://uob.iodt.ibm.com/univ-bench-dl.owl#subOrganizationOf'(Y,Z).
'http://www.example.org/fresh#Person_and_worksFor_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#worksFor_some_Organization'(X).
'http://www.example.org/fresh#Person_and_isCrazyAbout_some_Sports'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#isCrazyAbout_some_Sports'(X).
'http://www.example.org/fresh#Person_and_teachingAssistantOf_some_Course'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#teachingAssistantOf_some_Course'(X).
'http://www.example.org/fresh#isHeadOf_some_College'(X) :- 'http://www.example.org/fresh#isHeadOf_some_College'(X).
'http://www.example.org/fresh#Person_and_like_some_Sports'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#like_some_Sports'(X).
'http://www.example.org/fresh#Person_and_isStudentOf_some_Organization'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#isStudentOf_some_Organization'(X).
'http://www.example.org/fresh#Person_and_isHeadOf_some_Program'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#isHeadOf_some_Program'(X).
'http://www.example.org/fresh#Student_and_hasMajor_some_Science'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Student'(X),'http://www.example.org/fresh#hasMajor_some_Science'(X).
'http://www.example.org/fresh#Person_and_isHeadOf_some_Department'(X) :- 'http://uob.iodt.ibm.com/univ-bench-dl.owl#Person'(X),'http://www.example.org/fresh#isHeadOf_some_Department'(X).
