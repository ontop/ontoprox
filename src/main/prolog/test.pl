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






%:- dynamic http___www_example_org_fresh_fresh6/1.
%a

%http___www_example_org_fresh_fresh6(newconst).

%%%%%%%%%% An instance of a datalog program %%%%%%%%%%

edb(view(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_College(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_University(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_emailAddress(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_firstName(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_lastName(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_like(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_name(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_researchInterest(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_telephone(_,_)).
idb(http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(_,_)).
idb(http___www_example_org_fresh_fresh1(_)).
idb(http___www_example_org_fresh_fresh10(_)).
idb(http___www_example_org_fresh_fresh11(_)).
idb(http___www_example_org_fresh_fresh12(_)).
idb(http___www_example_org_fresh_fresh13(_)).
idb(http___www_example_org_fresh_fresh14(_)).
idb(http___www_example_org_fresh_fresh2(_)).
idb(http___www_example_org_fresh_fresh3(_)).
idb(http___www_example_org_fresh_fresh4(_)).
idb(http___www_example_org_fresh_fresh5(_)).
idb(http___www_example_org_fresh_fresh6(_)).
idb(http___www_example_org_fresh_fresh7(_)).
idb(http___www_example_org_fresh_fresh8(_)).
idb(http___www_example_org_fresh_fresh9(_)).

fresh(http___www_example_org_fresh_fresh1(_)).
fresh(http___www_example_org_fresh_fresh10(_)).
fresh(http___www_example_org_fresh_fresh11(_)).
fresh(http___www_example_org_fresh_fresh12(_)).
fresh(http___www_example_org_fresh_fresh13(_)).
fresh(http___www_example_org_fresh_fresh14(_)).
fresh(http___www_example_org_fresh_fresh2(_)).
fresh(http___www_example_org_fresh_fresh3(_)).
fresh(http___www_example_org_fresh_fresh4(_)).
fresh(http___www_example_org_fresh_fresh5(_)).
fresh(http___www_example_org_fresh_fresh6(_)).
fresh(http___www_example_org_fresh_fresh7(_)).
fresh(http___www_example_org_fresh_fresh8(_)).
fresh(http___www_example_org_fresh_fresh9(_)).

http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_University(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_University(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(X) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(X)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_emailAddress(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_emailAddress(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_firstName(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_firstName(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_lastName(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_lastName(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_name(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_name(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_researchInterest(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_researchInterest(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_telephone(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_telephone(X, Y)).
http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(X, Y) :- view(http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(X, Y)).

http___uob_iodt_ibm_com_univ_bench_dl_owl_AcademicSubject(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Engineering(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AcademicSubject(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_FineArts(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AcademicSubject(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_HumanitiesAndSocial(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AcademicSubject(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Science(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AcademicSubject(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X) :-  http___www_example_org_fresh_fresh6(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_WomanCollege(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateCourse(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_listedCourse(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_teacherOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Dean(X) :-  http___www_example_org_fresh_fresh1(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Director(X) :-  http___www_example_org_fresh_fresh14(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Faculty(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X), http___www_example_org_fresh_fresh4(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SupportingStaff(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Faculty(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Faculty(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_PostDoc(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Faculty(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Faculty(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateCourse(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(X,Y), http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Insterest(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Music(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Insterest(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Sports(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Institute(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Program(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_University(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___www_example_org_fresh__eliminatedtransfresh_0(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X) :-  http___www_example_org_fresh__eliminatedtransfresh_2(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isAffiliateOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isAffiliatedOrganizationOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isAffiliatedOrganizationOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isStudentOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_orgPublication(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Dean(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Director(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_PeopleWithHobby(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsFan(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsLover(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___www_example_org_fresh__eliminatedtransfresh_1(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :-  http___www_example_org_fresh__eliminatedtransfresh_3(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasAlumnus(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMember(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isAffiliateOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Dean(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_VisitingProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Professor(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_tenured(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_orgPublication(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationDate(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationResearch(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Publication(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_softwareDocumentation(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Research(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationResearch(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Research(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_researchProject(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_researchProject(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Schedule(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_listedCourse(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ScienceStudent(X) :-  http___www_example_org_fresh_fresh7(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_softwareDocumentation(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_softwareVersion(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsFan(X) :-  http___www_example_org_fresh_fresh8(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsLover(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X), http___www_example_org_fresh_fresh9(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_NonScienceStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ScienceStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X) :-  http___www_example_org_fresh_fresh5(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_SupportingStaff(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_SupportingStaff(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X), http___www_example_org_fresh_fresh13(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(X,Y), http___uob_iodt_ibm_com_univ_bench_dl_owl_WomanCollege(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_University(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_University(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_University(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_University(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Work(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Work(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Research(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasAlumnus(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasAlumnus(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasAlumnus(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasAlumnus(Y,X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDegreeFrom(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasAlumnus(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDegreeFrom(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDegreeFrom(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDegreeFrom(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMember(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMember(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMember(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(X,Z) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(X,Y), http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(Y,Z).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(Y,X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(Y,X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isStudentOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(Y,X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isStudentOf(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(Y,X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMember(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isStudentOf(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isStudentOf(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_teacherOf(Y,X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_love(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_love(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X,Z) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X,Y), http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(Y,Z).
http___uob_iodt_ibm_com_univ_bench_dl_owl_teacherOf(Y,X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X,Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(X,Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(X,Y).
http___www_example_org_fresh__eliminatedtransfresh_0(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X,Y).
http___www_example_org_fresh__eliminatedtransfresh_0(Y) :-  http___www_example_org_fresh__eliminatedtransfresh_0(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X,Y).
http___www_example_org_fresh__eliminatedtransfresh_1(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(Y,X), http___www_example_org_fresh__eliminatedtransfresh_1(X).
http___www_example_org_fresh__eliminatedtransfresh_1(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(Y,X).
http___www_example_org_fresh__eliminatedtransfresh_2(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(Y,X).
http___www_example_org_fresh__eliminatedtransfresh_2(Y) :-  http___www_example_org_fresh__eliminatedtransfresh_2(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(Y,X).
http___www_example_org_fresh__eliminatedtransfresh_3(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(X,Y), http___www_example_org_fresh__eliminatedtransfresh_3(X).
http___www_example_org_fresh__eliminatedtransfresh_3(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasSameHomeTownWith(X,Y).
http___www_example_org_fresh_fresh1(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Dean(X).
http___www_example_org_fresh_fresh1(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(Y,X), http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X).
http___www_example_org_fresh_fresh10(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(X,Y), http___uob_iodt_ibm_com_univ_bench_dl_owl_WomanCollege(X).
http___www_example_org_fresh_fresh11(X) :-  http___www_example_org_fresh_fresh10(X).
http___www_example_org_fresh_fresh12(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X).
http___www_example_org_fresh_fresh13(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X).
http___www_example_org_fresh_fresh13(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(Y,X).
http___www_example_org_fresh_fresh14(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Director(X).
http___www_example_org_fresh_fresh14(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(Y,X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Program(X).
http___www_example_org_fresh_fresh2(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_NonScienceStudent(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(X,Y).
http___www_example_org_fresh_fresh3(X) :-  http___www_example_org_fresh_fresh2(X).
http___www_example_org_fresh_fresh4(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X).
http___www_example_org_fresh_fresh4(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Dean(X).
http___www_example_org_fresh_fresh4(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Director(X).
http___www_example_org_fresh_fresh4(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee(X).
http___www_example_org_fresh_fresh4(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(X).
http___www_example_org_fresh_fresh4(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(Y,X).
http___www_example_org_fresh_fresh5(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Student(X).
http___www_example_org_fresh_fresh5(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_hasStudent(X,Y), http___uob_iodt_ibm_com_univ_bench_dl_owl_Organization(X).
http___www_example_org_fresh_fresh6(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X).
http___www_example_org_fresh_fresh6(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(Y,X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(X).
http___www_example_org_fresh_fresh7(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_ScienceStudent(X).
http___www_example_org_fresh_fresh7(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Science(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(Y,X).
http___www_example_org_fresh_fresh8(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsFan(X).
http___www_example_org_fresh_fresh8(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Sports(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(Y,X).
http___www_example_org_fresh_fresh9(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsFan(X).
http___www_example_org_fresh_fresh9(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_SportsLover(X).
http___www_example_org_fresh_fresh9(Y) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_like(Y,X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Sports(X).
http___www_w3_org_2002_07_owl_Nothing(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X), http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(X).
http___www_w3_org_2002_07_owl_Nothing(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X), http___www_example_org_fresh_fresh11(X).
http___www_w3_org_2002_07_owl_Nothing(X) :-  http___uob_iodt_ibm_com_univ_bench_dl_owl_Science(X), http___www_example_org_fresh_fresh3(X).
http___www_w3_org_2002_07_owl_Nothing(X) :-  http___www_example_org_fresh_eliminateMinCard_fresh3(X), http___www_example_org_fresh_eliminateMinCard_fresh5(X).
http___www_w3_org_2002_07_owl_Nothing(X) :-  http___www_example_org_fresh_eliminateMinCard_fresh4(X), http___www_example_org_fresh_eliminateMinCard_fresh3(X).
http___www_w3_org_2002_07_owl_Nothing(X) :-  http___www_example_org_fresh_eliminateMinCard_fresh4(X), http___www_example_org_fresh_eliminateMinCard_fresh5(X).
