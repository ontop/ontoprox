%:- dynamic tab_reach/2.
%:- dynamic reach/2.

node(1).
node(2).
node(3).

edge(1,2).
edge(2,3).
edge(3,4).
edge(4,5).
edge(2,4).
edge(3,1).


reach(X, Y) :-
  t_reach(X, Y, [X]).                   

t_reach(X, Y, L) :- 
  edge(X, Y), not(member(Y, L)).

t_reach(X, Y, IntermediateNodes) :-     
  edge(X, Z), not(member(Z, IntermediateNodes)),
  t_reach(Z, Y, [Z | IntermediateNodes]).


%reach(X, Y) :- edge(X, Y).
%reach(X, Y) :- edge(X, Z), reach(Z, Y).

%reach(X, Y) :- edge(X, Y), asserta(tab_reach(X, Y)).
%reach(X, Y) :- edge(X, Z), node(Y), (tab_reach(X, Y), ! ; reach(Z, Y), asserta(tab_reach(X, Y))) .