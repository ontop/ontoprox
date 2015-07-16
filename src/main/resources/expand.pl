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


