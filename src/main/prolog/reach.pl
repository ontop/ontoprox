%:- dynamic tab_reach/2.
%:- dynamic reach/2.

node(1).
node(2).
node(3).

edge(1,2).
edge(2,3).
edge(3,2).


reach(L1, L2) :-
  t_reach(L1, L2, [L1]).                   % <-- [L1] instead of []

t_reach(X, Y, L) :- edge(X, Y), L = [X].

t_reach(X1, X2, IntermediateNodes) :-      % <-- this cXause is unchanged
  edge(X1, X3),
  \+ member(X3, IntermediateNodes),
  t_reach(X3, X2, [X3 | IntermediateNodes]).


%reach(X, Y) :- edge(X, Y).
%reach(X, Y) :- edge(X, Z), reach(Z, Y).

%reach(X, Y) :- edge(X, Y), asserta(tab_reach(X, Y)).
%reach(X, Y) :- edge(X, Z), node(Y), (tab_reach(X, Y), ! ; reach(Z, Y), asserta(tab_reach(X, Y))) .