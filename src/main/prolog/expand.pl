%:- op(500, xfy, <==).
%expand(true,true).
%expand((Goal1,Goal2), (Expansion1, Expansion2)) 
%    :- expand(Goal1, Expansion1), expand(Goal2 , Expansion2).
%expand(call(X), Y) :- !, fail. % critical !!!

:- dynamic
        edge/2.

reach(X, Y) :- edge(X, Y).
%reach(X, Y) :- edge(X, Z), reach(Z, Y).
reach(X, Y) :- reach(X, Z), reach(Z, Y).
%reach(X, Y) :- edge(X, Z), edge(Z, W), reach(W, Y).

reach3(X, Y) :- edge(X, Z),  edge(Z, W), edge(W, Y).

edb(edge(_,_)).


expand(call(_), _, _) :- !, fail. % critical !!!

expand(Goal, Goal, _) :- edb(Goal), !. % EDB predicate should not be expanded. They will be defined by the mappings

expand(Goal, Goal, 0) :- !, fail. % this means you reach an IDB predicate, which should not be expanded further



expand((Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
    Depth >= 1, Depth_1 is Depth - 1,  expand(Goal1, Expansion1, Depth_1), expand(Goal2, Expansion2, Depth_1).



expand(Goal, Expansion, Depth) :- clause(Goal, Body),  expand(Body, Expansion, Depth).

expand_L(Goal, EL, Depth) :- expand(Goal, Expansion, Depth), ft(Expansion, EL).

ft(T, [T]) :- var(T), !.
ft(T, [T]) :- atomic(T), !.
ft((T1, T2), L) :- ft(T1, L1), ft(T2, L2), append(L1, L2, L), !.
ft(T, [T]) :- compound(T), !.


expands(G, Expansions, Depth) :- findall(Expansion, expand_L(G, Expansion, Depth), Expansions).


%% sorts lists of lists by length
%% adapted quick sort.

sortByLength([], []).  
sortByLength([H|T], S) :- 
    splitByLength(H, T, L, R), 
    sortByLength(L, LS), 
    sortByLength(R, RS), 
    append(LS, [H|RS], S).

splitByLength(_, [], [], []).  
splitByLength(H, [A|X], [A|Y], Z) :- 
    length(H, HL), length(A, AL), 
    AL =< HL, !, splitByLength(H, X, Y, Z).  
splitByLength(H, [A|X], Y, [A|Z]) :- 
    length(H, HL), length(A, AL), 
    AL > HL, !, splitByLength(H, X, Y, Z).



%% remove equivalent expansions

member(X,[Y|_]) :- X =@= Y, !.
member(X,[_|T]) :- member(X,T).

removeEquivalent([],[]).
removeEquivalent([H|T],[H|Out]) :-
    not(member(H,T)),
    removeEquivalent(T,Out).
removeEquivalent([H|T],Out) :-
    member(H,T),
    removeEquivalent(T,Out).


%% works also without sort
solutions(P, Depth, Unique) :- 
    findall(Expansion, expand_L(P, Expansion, Depth), Expansions), 
    removeEquivalent(Expansions, Unique).

%% with sort
%solutions(P, Depth, Unique) :- 
%    findall(Expansion, expand_L(P, Expansion, Depth), Expansions), 
%    sortByLength(Expansions, Sorted), 
%    removeEquivalent(Sorted, Unique).
