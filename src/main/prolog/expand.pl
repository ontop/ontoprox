:- dynamic
        edge/2.

:- dynamic
        edb/1.


reach(X, Y) :- edge(X, Y).
%reach(X, Y) :- edge(X, Z), reach(Z, Y).
reach(X, Y) :- reach(X, Z), reach(Z, Y).

start(X) :- reach(X, _).
end(Y) :- reach(_, Y).

person(X) :- chair(X).
chair(X) :- person(X), headOf(X, _).
%
% p(X) :- q(X).
% q(X) :- r(X).
% r(X) :- s(X).

edb(s(_)).
edb(edge(_,_)).

expand(call(_), _, _) :- !, fail. % critical !!!

expand(Goal, Goal, _) :- edb(Goal), !. % EDB predicate should not be expanded. They will be defined by the mappings

expand(Goal, Goal, 0) :- !, fail. % this means you reach an IDB predicate, which should not be expanded further

%expand((Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
%    Depth >= 1, Depth_1 is Depth - 1,  expand(Goal1, Expansion1, Depth_1), expand(Goal2, Expansion2, Depth_1).

expand((Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
    expand(Goal1, Expansion1, Depth), expand(Goal2, Expansion2, Depth).

expand(Goal, Expansion, Depth) :-
     clause(Goal, Body),  Depth >= 1, Depth_1 is Depth - 1, expand(Body, Expansion, Depth_1).


expand_list(Goal, GoalAndExpansion, Depth) :- expand(Goal, Expansion, Depth),
    flatten(Expansion, ExpansionList), GoalAndExpansion = (Goal, ExpansionList).

flatten(T, [T]) :- var(T), !.
flatten(T, [T]) :- atomic(T), !.
flatten((T1, T2), L) :- flatten(T1, L1), flatten(T2, L2), append(L1, L2, L), !.
flatten(T, [T]) :- compound(T), !.


%% remove redundant equivalent expansions

member(X,[Y|_]) :- X =@= Y, !.
member(X,[_|T]) :- member(X,T).

remove_equivalent([],[]).
remove_equivalent([H|T],[H|Out]) :-
    not(member(H,T)),
    remove_equivalent(T,Out).
remove_equivalent([H|T],Out) :-
    member(H,T),
    remove_equivalent(T,Out).

datalog_expansions(P, Depth, Expansions) :-
    findall(Expansion, expand_list(P, Expansion, Depth), IntermediateExpansions),
    remove_equivalent(IntermediateExpansions, Expansions).
