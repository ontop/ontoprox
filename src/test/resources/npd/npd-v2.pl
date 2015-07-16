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



edb(view(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#AppraisalWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#AwardArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#BAA'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#BAAArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#BAALicensee'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#BAATransfer'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Block'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#BlowoutWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ChangeOfCompanyNameTransfer'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Company'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#CompanyReserve'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ConcreteStructureFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Condeep3ShaftsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Condeep4ShaftsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#CondeepMonoshaftFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#CondensatePipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryReserve'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DorisFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ElectromagneticSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FPSOFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FSUFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FacilityPoint'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FeederPipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Field'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldInvestment'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldLicensee'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldMonthlyProduction'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldOperator'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldOwner'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldReserve'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldStatus'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FieldYearlyProduction'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#FixedFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Formation'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#GasPipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#GroundSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Group'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#InitialWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#InjectionWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#JackUp3LegsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#JackUp4LegsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Jacket12LegsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Jacket4LegsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Jacket6LegsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Jacket8LegsFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#JacketTripodFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#JunkedWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#LandfallFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#LoadingSystemFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MainArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Member'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MergerTakeoverTransfer'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MonotowerFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MopustorFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MoveableFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MultiFieldWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MultiWellTemplateFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#MultilateralWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#NCSMonthlyProduction'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#NCSYearlyProduction'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ObservationWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#OilGasPipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#OilPipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#OnshoreFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#OtherSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#PAWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ParcellBAA'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceAreaPerBlock'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceLicensee'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceOperator'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceStatus'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceTransfer'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ProductionWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Quadrant'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ReClassToDevWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ReClassToTestWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ReEntryWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#RegularSeismicSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#RouteSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SDFITransfer'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SeabedSeismicSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SeismicAreaBAA'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyCoordinate'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyProgress'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SemisubConcreteFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SemisubSteelFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ShallowWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SidetrackWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SingleWellTemplateFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SiteSurvey'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SlidingScaleBAA'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SubseaStructureFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SurveyArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SurveyMultilineArea'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SuspReenteredLaterWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SuspendedWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TUFOperator'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TUFOwner'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TUFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TlpConcreteFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TlpSteelFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TransportationPipeline'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#UnitizedBAA'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#VesselFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Well'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCasingAndLeakoffTest'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoordinate'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCorePhoto'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreDocument'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillStemTest'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillingMudSample'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreOilSample'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellborePoint'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratigraphicCoreSet'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratum'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#WildcatWellbore'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ChangedAwardNotification'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#DeletedEasementNotification'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#EasementNotification'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#NewAwardNotification'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicence'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceLicensee'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceNotification'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicensee'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFNotification'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTBottomHolePressure'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTChokeSize'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTDepthFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTDepthTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTFinalFlowPressure'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTFinalShutInPressure'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTGasDensity'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTGasOilRelation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTGasProducedRate'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTOilDensity'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTOilProducedRate'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#DSTTestNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#LOTForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#LOTMudDensity'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#SDFI'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#TUFOwnerShare'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ZValueFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ZValueTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#areaSize'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#areaSize3DKm2'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#baaLicensee'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#baaOperatorCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#baaTransferCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#belongsToFacility'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#belongsToWell'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#blockId'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#blockLocation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#casingDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#casingDiameter'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#casingType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#codeEW'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#codeNS'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#companyGroup'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#companyShare'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#containsWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coordinateForSurvey'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coordinateForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coreForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalBottom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalTop'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalUOM'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coreNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#corePhotoForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#corePhotoTitle'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#corePhotoURL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coresForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coresTotalLength'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#coresTotalNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#country'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#currentAreaSize'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOperator'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOwner'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#currentResponsibleCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#currentStatus'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateApproved'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaAreaValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaAreaValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaLicenseeValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaLicenseeValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldLicenseeFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldLicenseeTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOperatorFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOperatorTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOwnerFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOwnerTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateIncludedInField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateInitialStatusTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceAreaValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceAreaValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceGranted'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenseeValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenseeValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateMudMeasured'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateOilSampleReceived'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateOilSampleTest'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateOperatorValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateOperatorValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateProductionStart'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateResourceEstimate'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyActualCompleted'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyActualStart'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyPlannedCompleted'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyPlannedStart'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyProgress'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateSyncNPD'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOperatorValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOperatorValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOwnerValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOwnerValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateTaskExpiry'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateTransferValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateUpdated'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateUpdatedMax'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateWDSSQC'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateWellboreCompletion'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dateWellboreEntry'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#decimalDegreesEW'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#decimalDegreesNS'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#degreesEW'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#degreesNS'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#dependsOnTask'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#designLifetime'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#discoveryWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#discoveryYear'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#documentForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#documentFormat'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#documentName'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#documentSize'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#documentType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#documentURL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#drillPermit'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#drillingFacility'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#drillingOperatorCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#facilityFunction'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#facilityType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#factMapURL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#factPageURL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#fieldLicensee'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#fieldOperator'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#geochronologicEra'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#geodeticDatum'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#hydrocarbonType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#idNPD'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#inLithostratigraphicUnit'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#includedInField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#investmentForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#investmentNOK'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isActive'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isCoreSampleAvailable'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isCurrentLicenceLicensee'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isCurrentLicenceOperator'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isDiscoveryWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isFormerLicenceLicensee'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isFormerLicenceOperator'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isGeotechnicalMeasurement'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isMarketAvailable'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isMultilateral'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isReentryWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isSamplingPerformed'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isShallowDrillingPerformed'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isStratigraphical'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isSurfaceFacility'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#isTurnAreaIncluded'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#lastOperatorCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#lengthBoatTotalKm'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#lengthCdpTotalKm'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#lengthPlannedBoatKm'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#lengthPlannedCdpKm'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenceLicensee'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenceOperatorCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenceTransferCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenseeForBAA'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenseeForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenseeForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licenseeInterest'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#licensingActivityName'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mainAreaLocation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mapNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#member'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#messageForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#minutesEW'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#minutesNS'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mudMeasuredDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mudTestForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mudType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mudViscosity'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mudWeight'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#mudYieldPoint'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#name'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#npdPageURL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleBottomDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleFluidType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestNumber'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTopDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#operatorForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#operatorForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#originalAreaSize'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#ownerForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pipelineDimension'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pipelineFromFacility'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pipelineMainGrouping'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pipelineMedium'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pipelineOperator'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pipelineToFacility'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#polygonNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#polygonPointNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#pressReleaseURL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#producedCondensate'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#producedGas'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#producedNGL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#producedOil'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#producedOilEquivalents'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#producedWater'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#productionFacility'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#productionForDiscovery'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#productionForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#productionMonth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#productionYear'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#provinceLocation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#quadrantLocation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#reclassedFromWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#recoverableCondensate'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#recoverableGas'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#recoverableNGL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#recoverableOil'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#recoverableOilEquivalents'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#registeredInCountry'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#remainingCondensate'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#remainingGas'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#remainingNGL'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#remainingOil'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#remainingOilEquivalents'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#reportingCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#reservesForCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#reservesForDiscovery'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#reservesForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#reservesResourceClass'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#resourcesIncludedInDiscovery'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#secondsEW'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#secondsNS'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sensorLength'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sensorNumbers'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sensorType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#shallowWellboreForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#shortName'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sourceNumber'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sourcePressure'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sourceSize'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#sourceType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#status'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#statusForField'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#statusForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#statusForSurvey'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicLevel'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#stratumForWellbore'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#surveySubType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#surveyType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#taskForCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#taskForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#taskID'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#taskType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#transferDirection'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#transferredBAA'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#transferredInterest'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#transferredLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#utmEW'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#utmNS'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#utmZone'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#waterDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellOperator'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel1'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel2'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel3'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeTD'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreBottomHoleTemperature'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreCompletionYear'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreContent'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreDrillingDays'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreEntryYear'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFinalVerticalDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreForDiscovery'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel1'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel2'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel3'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationTD'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreHoleDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreHoleDiameter'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreKellyBushElevation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreLicensingActivity'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreMaxInclation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart1'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart2'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart3'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart4'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart5'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart6'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreParent'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellborePlannedContent'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellborePlannedPurpose'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellborePlotSymbol'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellborePurpose'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreReentryExplorationActivity'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreSeismicLocation'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreStratumBottomDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreStratumTopDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreTotalDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreWaterDepth'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2#wellboreWellType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeForTUF'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateAwarded'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateLicenceValidFrom'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateLicenceValidTo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateMessageRegistered'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateUpdated'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateUpdatedMax'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceLicenceeCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceOperatorCompany'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceeForLicence'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenseeInterest'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageForTUF'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageNo'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageType'(_,_)).
idb('http://sws.ifi.uio.no/vocab/npd-v2-ptl#name'(_,_)).

'http://sws.ifi.uio.no/vocab/npd-v2#AppraisalWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#AppraisalWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#AwardArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#AwardArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#BAAArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#BAAArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#BAALicensee'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#BAALicensee'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#BAATransfer'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#BAATransfer'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Block'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Block'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#BlowoutWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#BlowoutWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ChangeOfCompanyNameTransfer'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ChangeOfCompanyNameTransfer'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Company'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#CompanyReserve'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#CompanyReserve'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ConcreteStructureFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ConcreteStructureFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Condeep3ShaftsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Condeep3ShaftsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Condeep4ShaftsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Condeep4ShaftsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#CondeepMonoshaftFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#CondeepMonoshaftFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#CondensatePipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#CondensatePipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryReserve'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryReserve'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#DorisFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DorisFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ElectromagneticSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ElectromagneticSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FPSOFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FPSOFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FSUFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FSUFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FacilityPoint'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FacilityPoint'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FeederPipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FeederPipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Field'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldInvestment'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldInvestment'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldLicensee'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldLicensee'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldMonthlyProduction'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldMonthlyProduction'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldOperator'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldOperator'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldOwner'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldOwner'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldReserve'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldReserve'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldStatus'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldStatus'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldYearlyProduction'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FieldYearlyProduction'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#FixedFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#FixedFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Formation'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Formation'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#GasPipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#GasPipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#GroundSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#GroundSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Group'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Group'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#InitialWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#InitialWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#InjectionWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#InjectionWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#JackUp3LegsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#JackUp3LegsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#JackUp4LegsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#JackUp4LegsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Jacket12LegsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Jacket12LegsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Jacket4LegsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Jacket4LegsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Jacket6LegsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Jacket6LegsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Jacket8LegsFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Jacket8LegsFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#JacketTripodFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#JacketTripodFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#JunkedWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#JunkedWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#LandfallFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#LandfallFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#LoadingSystemFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#LoadingSystemFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MainArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MainArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Member'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Member'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MergerTakeoverTransfer'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MergerTakeoverTransfer'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MonotowerFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MonotowerFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MopustorFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MopustorFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MoveableFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MoveableFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MultiFieldWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MultiFieldWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MultiWellTemplateFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MultiWellTemplateFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#MultilateralWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#MultilateralWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#NCSMonthlyProduction'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#NCSMonthlyProduction'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#NCSYearlyProduction'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#NCSYearlyProduction'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ObservationWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ObservationWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#OilGasPipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#OilGasPipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#OilPipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#OilPipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#OnshoreFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#OnshoreFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#OtherSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#OtherSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#PAWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#PAWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ParcellBAA'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ParcellBAA'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceAreaPerBlock'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceAreaPerBlock'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceLicensee'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceLicensee'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceOperator'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceOperator'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceStatus'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceStatus'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceTransfer'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceTransfer'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ProductionWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Quadrant'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Quadrant'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ReClassToDevWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ReClassToDevWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ReClassToTestWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ReClassToTestWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ReEntryWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ReEntryWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#RegularSeismicSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#RegularSeismicSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#RouteSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#RouteSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SDFITransfer'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SDFITransfer'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SeabedSeismicSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SeabedSeismicSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicAreaBAA'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SeismicAreaBAA'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyCoordinate'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyCoordinate'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyProgress'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyProgress'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SemisubConcreteFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SemisubConcreteFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SemisubSteelFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SemisubSteelFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#ShallowWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ShallowWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SidetrackWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SidetrackWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SingleWellTemplateFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SingleWellTemplateFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SiteSurvey'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SiteSurvey'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SlidingScaleBAA'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SlidingScaleBAA'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SubseaStructureFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SubseaStructureFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SurveyArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SurveyArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SurveyMultilineArea'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SurveyMultilineArea'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SuspReenteredLaterWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SuspReenteredLaterWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#SuspendedWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SuspendedWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFOperator'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TUFOperator'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFOwner'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TUFOwner'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TUFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#TlpConcreteFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TlpConcreteFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#TlpSteelFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TlpSteelFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#TransportationPipeline'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TransportationPipeline'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#UnitizedBAA'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#UnitizedBAA'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#VesselFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#VesselFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Well'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCasingAndLeakoffTest'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCasingAndLeakoffTest'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoordinate'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoordinate'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCorePhoto'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCorePhoto'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDocument'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreDocument'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillStemTest'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillStemTest'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillingMudSample'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillingMudSample'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreOilSample'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreOilSample'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellborePoint'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellborePoint'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratigraphicCoreSet'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratigraphicCoreSet'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratum'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratum'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#WildcatWellbore'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2#WildcatWellbore'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ChangedAwardNotification'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ChangedAwardNotification'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#DeletedEasementNotification'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#DeletedEasementNotification'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#EasementNotification'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#EasementNotification'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#NewAwardNotification'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#NewAwardNotification'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicence'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicence'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceLicensee'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceLicensee'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceNotification'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceNotification'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicensee'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicensee'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFNotification'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFNotification'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(X) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(X)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTBottomHolePressure'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTBottomHolePressure'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTChokeSize'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTChokeSize'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTDepthFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTDepthFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTDepthTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTDepthTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTFinalFlowPressure'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTFinalFlowPressure'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTFinalShutInPressure'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTFinalShutInPressure'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTGasDensity'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTGasDensity'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTGasOilRelation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTGasOilRelation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTGasProducedRate'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTGasProducedRate'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTOilDensity'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTOilDensity'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTOilProducedRate'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTOilProducedRate'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#DSTTestNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#DSTTestNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#LOTForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#LOTForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#LOTMudDensity'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#LOTMudDensity'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#SDFI'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#SDFI'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFOwnerShare'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#TUFOwnerShare'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#ZValueFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ZValueFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#ZValueTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ZValueTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#areaSize'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#areaSize'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#areaSize3DKm2'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#areaSize3DKm2'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#baaLicensee'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#baaLicensee'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#baaOperatorCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#baaOperatorCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#baaTransferCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#baaTransferCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#belongsToFacility'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#belongsToFacility'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#belongsToWell'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#belongsToWell'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#blockId'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#blockId'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#blockLocation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#blockLocation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#casingDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#casingDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#casingDiameter'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#casingDiameter'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#casingType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#casingType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#codeEW'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#codeEW'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#codeNS'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#codeNS'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#companyGroup'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#companyGroup'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#companyShare'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#companyShare'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#containsWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#containsWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coordinateForSurvey'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coordinateForSurvey'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coordinateForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coordinateForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coreForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coreForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalBottom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalBottom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalTop'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalTop'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalUOM'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coreIntervalUOM'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coreNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coreNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#corePhotoForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#corePhotoForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#corePhotoTitle'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#corePhotoTitle'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#corePhotoURL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#corePhotoURL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coresForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coresForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coresTotalLength'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coresTotalLength'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#coresTotalNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#coresTotalNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#country'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#country'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#currentAreaSize'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#currentAreaSize'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOperator'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOperator'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOwner'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOwner'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#currentResponsibleCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#currentResponsibleCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#currentStatus'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#currentStatus'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateApproved'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateApproved'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateBaaAreaValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaAreaValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateBaaAreaValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaAreaValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateBaaLicenseeValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaLicenseeValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateBaaLicenseeValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaLicenseeValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateBaaValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateBaaValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateBaaValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateFieldLicenseeFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldLicenseeFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateFieldLicenseeTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldLicenseeTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOperatorFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOperatorFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOperatorTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOperatorTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOwnerFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOwnerFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOwnerTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateFieldOwnerTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateIncludedInField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateIncludedInField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateInitialStatusTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateInitialStatusTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceAreaValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceAreaValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceAreaValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceAreaValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceGranted'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceGranted'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenceValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateLicenseeValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenseeValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateLicenseeValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateLicenseeValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateMudMeasured'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateMudMeasured'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateOilSampleReceived'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateOilSampleReceived'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateOilSampleTest'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateOilSampleTest'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateOperatorValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateOperatorValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateOperatorValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateOperatorValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateProductionStart'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateProductionStart'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateResourceEstimate'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateResourceEstimate'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateStatusFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateStatusTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateStatusValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateStatusValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateStatusValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyActualCompleted'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyActualCompleted'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyActualStart'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyActualStart'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyPlannedCompleted'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyPlannedCompleted'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyPlannedStart'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyPlannedStart'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyProgress'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateSurveyProgress'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateSyncNPD'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateSyncNPD'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOperatorValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOperatorValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOperatorValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOperatorValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOwnerValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOwnerValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOwnerValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateTUFOwnerValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateTaskExpiry'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateTaskExpiry'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateTransferValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateTransferValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateUpdated'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateUpdated'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateUpdatedMax'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateUpdatedMax'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateWDSSQC'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateWDSSQC'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateWellboreCompletion'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateWellboreCompletion'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dateWellboreEntry'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dateWellboreEntry'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#decimalDegreesEW'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#decimalDegreesEW'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#decimalDegreesNS'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#decimalDegreesNS'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#degreesEW'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#degreesEW'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#degreesNS'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#degreesNS'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#dependsOnTask'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#dependsOnTask'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#designLifetime'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#designLifetime'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#discoveryWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#discoveryWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#discoveryYear'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#discoveryYear'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#documentForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#documentForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#documentFormat'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#documentFormat'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#documentName'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#documentName'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#documentSize'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#documentSize'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#documentType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#documentType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#documentURL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#documentURL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#drillPermit'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#drillPermit'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#drillingFacility'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#drillingFacility'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#drillingOperatorCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#drillingOperatorCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#facilityFunction'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#facilityFunction'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#facilityType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#facilityType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#factMapURL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#factMapURL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#factPageURL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#factPageURL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#fieldLicensee'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#fieldLicensee'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#fieldOperator'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#fieldOperator'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#geochronologicEra'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#geochronologicEra'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#geodeticDatum'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#geodeticDatum'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#hydrocarbonType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#hydrocarbonType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#idNPD'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#idNPD'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#inLithostratigraphicUnit'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#inLithostratigraphicUnit'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#includedInField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#includedInField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#investmentForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#investmentForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#investmentNOK'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#investmentNOK'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isActive'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isActive'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isCoreSampleAvailable'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isCoreSampleAvailable'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isCurrentLicenceLicensee'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isCurrentLicenceLicensee'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isCurrentLicenceOperator'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isCurrentLicenceOperator'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isDiscoveryWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isDiscoveryWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isFormerLicenceLicensee'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isFormerLicenceLicensee'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isFormerLicenceOperator'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isFormerLicenceOperator'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isGeotechnicalMeasurement'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isGeotechnicalMeasurement'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isMarketAvailable'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isMarketAvailable'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isMultilateral'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isMultilateral'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isReentryWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isReentryWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isSamplingPerformed'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isSamplingPerformed'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isShallowDrillingPerformed'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isShallowDrillingPerformed'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isStratigraphical'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isStratigraphical'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isSurfaceFacility'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isSurfaceFacility'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#isTurnAreaIncluded'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#isTurnAreaIncluded'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#lastOperatorCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#lastOperatorCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#lengthBoatTotalKm'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#lengthBoatTotalKm'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#lengthCdpTotalKm'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#lengthCdpTotalKm'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#lengthPlannedBoatKm'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#lengthPlannedBoatKm'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#lengthPlannedCdpKm'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#lengthPlannedCdpKm'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenceLicensee'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenceLicensee'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenceOperatorCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenceOperatorCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenceTransferCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenceTransferCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForBAA'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenseeForBAA'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenseeForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenseeForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licenseeInterest'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licenseeInterest'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#licensingActivityName'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#licensingActivityName'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mainAreaLocation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mainAreaLocation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mapNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mapNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#member'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#member'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#messageForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#messageForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#minutesEW'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#minutesEW'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#minutesNS'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#minutesNS'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mudMeasuredDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mudMeasuredDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mudTestForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mudTestForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mudType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mudType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mudViscosity'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mudViscosity'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mudWeight'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mudWeight'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#mudYieldPoint'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#mudYieldPoint'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#name'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#name'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#npdPageURL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#npdPageURL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleBottomDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleBottomDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleFluidType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleFluidType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestNumber'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestNumber'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTopDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTopDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#operatorForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#operatorForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#operatorForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#operatorForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#originalAreaSize'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#originalAreaSize'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#ownerForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#ownerForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pipelineDimension'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pipelineDimension'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pipelineFromFacility'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pipelineFromFacility'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pipelineMainGrouping'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pipelineMainGrouping'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pipelineMedium'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pipelineMedium'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pipelineOperator'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pipelineOperator'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pipelineToFacility'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pipelineToFacility'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#polygonNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#polygonNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#polygonPointNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#polygonPointNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#pressReleaseURL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#pressReleaseURL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#producedCondensate'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#producedCondensate'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#producedGas'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#producedGas'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#producedNGL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#producedNGL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#producedOil'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#producedOil'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#producedOilEquivalents'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#producedOilEquivalents'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#producedWater'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#producedWater'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#productionFacility'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#productionFacility'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#productionForDiscovery'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#productionForDiscovery'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#productionForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#productionForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#productionMonth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#productionMonth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#productionYear'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#productionYear'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#provinceLocation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#provinceLocation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#quadrantLocation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#quadrantLocation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#reclassedFromWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#reclassedFromWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#recoverableCondensate'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#recoverableCondensate'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#recoverableGas'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#recoverableGas'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#recoverableNGL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#recoverableNGL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#recoverableOil'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#recoverableOil'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#recoverableOilEquivalents'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#recoverableOilEquivalents'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#registeredInCountry'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#registeredInCountry'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#remainingCondensate'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#remainingCondensate'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#remainingGas'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#remainingGas'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#remainingNGL'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#remainingNGL'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#remainingOil'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#remainingOil'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#remainingOilEquivalents'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#remainingOilEquivalents'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#reportingCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#reportingCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#reservesForCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#reservesForCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#reservesForDiscovery'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#reservesForDiscovery'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#reservesForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#reservesForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#reservesResourceClass'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#reservesResourceClass'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#resourcesIncludedInDiscovery'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#resourcesIncludedInDiscovery'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#secondsEW'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#secondsEW'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#secondsNS'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#secondsNS'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sensorLength'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sensorLength'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sensorNumbers'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sensorNumbers'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sensorType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sensorType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#shallowWellboreForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#shallowWellboreForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#shortName'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#shortName'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sourceNumber'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sourceNumber'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sourcePressure'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sourcePressure'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sourceSize'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sourceSize'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#sourceType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#sourceType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#status'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#status'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#statusForField'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#statusForField'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#statusForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#statusForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#statusForSurvey'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#statusForSurvey'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicLevel'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicLevel'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#stratumForWellbore'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#stratumForWellbore'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#surveySubType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#surveySubType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#surveyType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#surveyType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#taskForCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#taskForCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#taskForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#taskForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#taskID'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#taskID'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#taskType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#taskType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#transferDirection'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#transferDirection'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#transferredBAA'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#transferredBAA'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#transferredInterest'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#transferredInterest'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#transferredLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#transferredLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#utmEW'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#utmEW'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#utmNS'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#utmNS'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#utmZone'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#utmZone'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#waterDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#waterDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellOperator'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellOperator'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel1'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel1'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel2'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel2'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel3'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel3'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeTD'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeTD'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreBottomHoleTemperature'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreBottomHoleTemperature'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreCompletionYear'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreCompletionYear'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreContent'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreContent'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreDrillingDays'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreDrillingDays'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreEntryYear'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreEntryYear'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFinalVerticalDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFinalVerticalDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForDiscovery'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreForDiscovery'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel1'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel1'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel2'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel2'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel3'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel3'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationTD'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationTD'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreHoleDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreHoleDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreHoleDiameter'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreHoleDiameter'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreKellyBushElevation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreKellyBushElevation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreLicensingActivity'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreLicensingActivity'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreMaxInclation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreMaxInclation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart1'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart1'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart2'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart2'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart3'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart3'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart4'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart4'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart5'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart5'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart6'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreNamePart6'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreParent'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreParent'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellborePlannedContent'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellborePlannedContent'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellborePlannedPurpose'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellborePlannedPurpose'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellborePlotSymbol'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellborePlotSymbol'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellborePurpose'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellborePurpose'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreReentryExplorationActivity'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreReentryExplorationActivity'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreSeismicLocation'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreSeismicLocation'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreStratumBottomDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreStratumBottomDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreStratumTopDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreStratumTopDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreTotalDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreTotalDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreWaterDepth'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreWaterDepth'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreWellType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2#wellboreWellType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeForTUF'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeForTUF'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateAwarded'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateAwarded'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateLicenceValidFrom'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateLicenceValidFrom'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateLicenceValidTo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateLicenceValidTo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateMessageRegistered'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateMessageRegistered'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateUpdated'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateUpdated'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateUpdatedMax'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#dateUpdatedMax'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceLicenceeCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceLicenceeCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceOperatorCompany'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceOperatorCompany'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceeForLicence'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceeForLicence'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenseeInterest'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenseeInterest'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageForTUF'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageForTUF'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageNo'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageNo'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageType'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageType'(X, Y)).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#name'(X, Y) :- view('http://sws.ifi.uio.no/vocab/npd-v2-ptl#name'(X, Y)).
fresh('http://www.example.org/fresh#fresh3'(_)).
fresh('http://www.example.org/fresh#fresh11'(_)).
fresh('http://www.example.org/fresh#fresh9'(_)).
fresh('http://www.example.org/fresh#fresh15'(_)).
fresh('http://www.example.org/fresh#fresh6'(_)).
fresh('http://www.example.org/fresh#fresh2'(_)).
fresh('http://www.example.org/fresh#fresh5'(_)).
fresh('http://www.example.org/fresh#fresh12'(_)).
fresh('http://www.example.org/fresh#fresh14'(_)).
fresh('http://www.example.org/fresh#fresh13'(_)).
fresh('http://www.example.org/fresh#fresh1'(_)).
fresh('http://www.example.org/fresh#fresh4'(_)).
fresh('http://www.example.org/fresh#fresh10'(_)).
fresh('http://www.example.org/fresh#fresh7'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_0'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_3'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_1'(_)).
fresh('http://www.example.org/fresh#_eliminatedtransfresh_2'(_)).
fresh('http://www.example.org/fresh#fresh8'(_)).


'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Block'(X).
'http://www.w3.org/2004/02/skos/core#Concept'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_0'(X).
'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ScatteredSpatiotemporalRegion'(X).
'http://www.example.org/fresh#fresh3'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalRegion'(X).
'http://www.opengis.net/ont/gml#Surface'(X) :- 'http://www.opengis.net/ont/gml#PolyhedralSurface'(X).
'http://www.ifomis.org/bfo/1.1#Entity'(X) :- 'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ZeroDimensionalRegion'(X),'http://www.ifomis.org/bfo/1.1/snap#OneDimensionalRegion'(X).
'http://www.opengis.net/ont/sf#GeometryCollection'(X) :- 'http://www.opengis.net/ont/sf#MultiPoint'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X),'http://www.ifomis.org/bfo/1.1/span#TemporalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessAggregate'(X),'http://www.ifomis.org/bfo/1.1/span#FiatProcessPart'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#JacketTripodFacility'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectBoundary'(X),'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.w3.org/2004/02/skos/core#ConceptScheme'(X),'http://www.w3.org/2004/02/skos/core#Concept'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://www.opengis.net/ont/gml#AbstractSurfacePatch'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation'(X).
'http://www.opengis.net/ont/sf#MultiSurface'(X) :- 'http://www.opengis.net/ont/sf#MultiPolygon'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#OffsetCurve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DorisFacility'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#ArcString'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Refining'(X).
'http://www.opengis.net/ont/gml#Composite'(X) :- 'http://www.opengis.net/ont/gml#CompositeSolid'(X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(X) :- 'http://www.opengis.net/ont/geosparql#Geometry'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#Process'(X),'http://www.ifomis.org/bfo/1.1/span#FiatProcessPart'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LandfallFacility'(X).
'http://www.opengis.net/ont/gml#ArcStringByBulge'(X) :- 'http://www.opengis.net/ont/gml#ArcByBulge'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MergerTakeoverTransfer'(X).
'http://www.opengis.net/ont/gml#AbstractGriddedSurface'(X) :- 'http://www.opengis.net/ont/gml#Cone'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyProgress'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessAggregate'(X),'http://www.ifomis.org/bfo/1.1/span#Process'(X).
'http://www.opengis.net/ont/gml#Solid'(X) :- 'http://www.opengis.net/ont/gml#CompositeSolid'(X).
'http://www.ifomis.org/bfo/1.1/snap#Site'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Play'(X).
'http://www.opengis.net/ont/gml#AbstractGeometry'(X) :- 'http://www.opengis.net/ont/gml#MultiGeometry'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Oil'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#CrudeOil'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceAreaPerBlock'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceAreaPerBlock'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Plan'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#PlanForDevelopmentAndOperationOfPetroleumDeposits'(X).
'http://www.ifomis.org/bfo/1.1/snap#Object'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCorePhoto'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ZeroDimensionalRegion'(X),'http://www.ifomis.org/bfo/1.1/snap#TwoDimensionalRegion'(X).
'http://www.ifomis.org/bfo/1.1/span#TemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ConnectedTemporalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#RichGas'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#PetroleumActivity'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Petroleum'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Oil'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#FiatProcessPart'(X),'http://www.ifomis.org/bfo/1.1/span#ProcessBoundary'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ObservationWell'(X).
'http://www.opengis.net/ont/gml#SplineCurve'(X) :- 'http://www.opengis.net/ont/gml#PolynomialSpline'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessAggregate'(X),'http://www.ifomis.org/bfo/1.1/span#ProcessBoundary'(X).
'http://www.opengis.net/ont/gml#PolyhedralSurface'(X) :- 'http://www.opengis.net/ont/gml#TriangulatedSurface'(X).
'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Site'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Jacket12LegsFacility'(X).
'http://www.opengis.net/ont/gml#MultiGeometry'(X) :- 'http://www.opengis.net/ont/gml#MultiSolid'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://www.ifomis.org/bfo/1.1/span#TemporalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#JunkedWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TestProductionWell'(X).
'http://www.opengis.net/ont/gml#ArcString'(X) :- 'http://www.opengis.net/ont/gml#Arc'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MoveableFacility'(X).
'http://www.ifomis.org/bfo/1.1/span#ConnectedTemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#TemporalInstant'(X).
'http://www.example.org/fresh#fresh11'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ConnectedSpatiotemporalRegion'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#Process'(X),'http://www.ifomis.org/bfo/1.1/span#ProcessBoundary'(X).
'http://www.opengis.net/ont/gml#AbstractParametricCurveSurface'(X) :- 'http://www.opengis.net/ont/gml#AbstractGriddedSurface'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#UnitizedBAA'(X).
'http://www.opengis.net/ont/gml#PolygonPatch'(X) :- 'http://www.opengis.net/ont/gml#Triangle'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#SplineCurve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TUFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#OilEquivalents'(X).
'http://www.ifomis.org/bfo/1.1/span#TemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ScatteredTemporalRegion'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldMonthlyProduction'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SuspendedWellbore'(X).
'http://www.opengis.net/ont/sf#Surface'(X) :- 'http://www.opengis.net/ont/sf#Polygon'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Site'(X),'http://www.ifomis.org/bfo/1.1/snap#Object'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SeabedSeismicSurvey'(X).
'http://www.w3.org/2004/02/skos/core#Collection'(X) :- 'http://www.w3.org/2004/02/skos/core#OrderedCollection'(X).
'http://www.ifomis.org/bfo/1.1/snap#Quality'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceStatus'(X).
'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Quadrant'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://www.opengis.net/ont/sf#Geometry'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SiteSurvey'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#InjectionWellbore'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Recovery'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ReClassToTestWellbore'(X).
'http://www.example.org/fresh#fresh9'(X) :- 'http://www.example.org/fresh#fresh8'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TechnicalDiscovery'(X).
'http://www.ifomis.org/bfo/1.1#Entity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWell'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#KickOffPoint'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWell'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FacilityPoint'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Award'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Member'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ConnectedSpatiotemporalRegion'(X),'http://www.ifomis.org/bfo/1.1/span#ScatteredSpatiotemporalRegion'(X).
'http://www.opengis.net/ont/gml#GeometricComplex'(X) :- 'http://www.opengis.net/ont/gml#Composite'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Member'(X),'http://sws.ifi.uio.no/vocab/npd-v2#Group'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SemisubConcreteFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreOilSample'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Condensate'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SingleWellTemplateFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Owner'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TUFOwner'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#OnshoreFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#AbandonedWell'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Disposition'(X),'http://www.ifomis.org/bfo/1.1/snap#Function'(X).
'http://www.opengis.net/ont/gml#CompositeCurve'(X) :- 'http://www.opengis.net/ont/gml#Ring'(X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/span#FiatProcessPart'(X).
'http://www.ifomis.org/bfo/1.1/span#ConnectedTemporalRegion'(X) :- 'http://resource.geosciml.org/classifier/ics/ischart/GeochronologicEra'(X).
'http://www.example.org/fresh#fresh15'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#RecoveryWell'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.opengis.net/ont/geosparql#Feature'(X),'http://www.opengis.net/ont/geosparql#Geometry'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessAggregate'(X).
'http://www.ifomis.org/bfo/1.1/snap#RealizableEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Function'(X).
'http://www.opengis.net/ont/gml#Arc'(X) :- 'http://www.opengis.net/ont/gml#Circle'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Petroleum'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#NaturalGasLiquid'(X).
'http://www.opengis.net/ont/sf#Geometry'(X) :- 'http://www.opengis.net/ont/sf#GeometryCollection'(X).
'http://www.example.org/fresh#fresh6'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ConnectedTemporalRegion'(X).
'http://www.opengis.net/ont/gml#CompositeSurface'(X) :- 'http://www.opengis.net/ont/gml#Shell'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SurveyArea'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SurveyArea'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BAATransfer'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#JackUp4LegsFacility'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Kick'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Licensee'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceLicensee'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/span#Process'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#CommercialDiscovery'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Sidetrack'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TechnicalSideTrack'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Petroleum'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X).
'http://www.example.org/fresh#fresh2'(X) :- 'http://www.ifomis.org/bfo/1.1#Entity'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FSUFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Plan'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TerminationPlan'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Plan'(X).
'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Petroleum'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Disposition'(X),'http://www.ifomis.org/bfo/1.1/snap#Role'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#NCSMonthlyProduction'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Owner'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldOwner'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Member'(X),'http://sws.ifi.uio.no/vocab/npd-v2#Formation'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Owner'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessBoundary'(X).
'http://www.ifomis.org/bfo/1.1/snap#RealizableEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Role'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Point'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FacilityPoint'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#GasPipeline'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FPSOFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LoadingSystemFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Equipment'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Riser'(X).
'http://www.example.org/fresh#fresh5'(X) :- 'http://www.ifomis.org/bfo/1.1/span#TemporalRegion'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#RealizableEntity'(X),'http://www.ifomis.org/bfo/1.1/snap#Quality'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratum'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#RouteSurvey'(X).
'http://www.opengis.net/ont/sf#Geometry'(X) :- 'http://www.opengis.net/ont/sf#Surface'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectBoundary'(X),'http://www.ifomis.org/bfo/1.1/snap#FiatObjectPart'(X).
'http://www.ifomis.org/bfo/1.1/snap#Quality'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldStatus'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X).
'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X) :- 'http://www.opengis.net/ont/gml#OrientableSurface'(X).
'http://www.ifomis.org/bfo/1.1/snap#Object'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Equipment'(X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#Notification'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#NewAwardNotification'(X).
'http://www.opengis.net/ont/gml#MultiGeometry'(X) :- 'http://www.opengis.net/ont/gml#MultiCurve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SemisubSteelFacility'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldYearlyProduction'(X).
'http://www.opengis.net/ont/sf#Polygon'(X) :- 'http://www.opengis.net/ont/sf#Triangle'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Site'(X),'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellborePoint'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FeederPipeline'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Jacket8LegsFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ChangeOfCompanyNameTransfer'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#CondensatePipeline'(X).
'http://www.example.org/fresh#fresh12'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X).
'http://www.ifomis.org/bfo/1.1/span#ConnectedTemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#TemporalInterval'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Point'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoordinate'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X).
'http://www.opengis.net/ont/sf#LineString'(X) :- 'http://www.opengis.net/ont/sf#Line'(X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#Notification'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#DeletedEasementNotification'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultipurposeWellbore'(X).
'http://www.opengis.net/ont/gml#OrientableSurface'(X) :- 'http://www.opengis.net/ont/gml#CompositeSurface'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://www.opengis.net/ont/gml#AbstractGeometry'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Operator'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldOperator'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X),'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ReClassToDevWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SlidingScaleBAA'(X).
'http://www.opengis.net/ont/gml#AbstractGeometry'(X) :- 'http://www.opengis.net/ont/gml#GeometricComplex'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Reserve'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldReserve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SDFITransfer'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#VesselFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ShallowWellbore'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#ArcStringByBulge'(X).
'http://www.ifomis.org/bfo/1.1/snap#SpecificallyDependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Quality'(X).
'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#GenericallyDependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Licensee'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BAALicensee'(X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Point'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellTrack'(X).
'http://www.opengis.net/ont/gml#MultiGeometry'(X) :- 'http://www.opengis.net/ont/gml#MultiSurface'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Jacket4LegsFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#RegularSeismicSurvey'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Company'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Condeep4ShaftsFacility'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalRegion'(X),'http://www.ifomis.org/bfo/1.1/span#TemporalRegion'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.w3.org/2004/02/skos/core#ConceptScheme'(X),'http://www.w3.org/2004/02/skos/core#Collection'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDocument'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceTransfer'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WildcatWell'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultibranchWell'(X).
'http://www.opengis.net/ont/sf#MultiCurve'(X) :- 'http://www.opengis.net/ont/sf#MultiLineString'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BlowoutWellbore'(X).
'http://www.ifomis.org/bfo/1.1/span#TemporalInstant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BlowOut'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X),'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Group'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WildcatWellbore'(X).
'http://www.opengis.net/ont/gml#AbstractGeometry'(X) :- 'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X).
'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#FiatObjectPart'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillingMudSample'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#PAWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Point'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellborePoint'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LiquefiedPetroleumGas'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TlpSteelFacility'(X).
'http://www.opengis.net/ont/gml#MultiGeometry'(X) :- 'http://www.opengis.net/ont/gml#MultiPoint'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ReEntryWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicence'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Object'(X),'http://www.ifomis.org/bfo/1.1/snap#FiatObjectPart'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(X).
'http://www.opengis.net/ont/sf#GeometryCollection'(X) :- 'http://www.opengis.net/ont/sf#MultiCurve'(X).
'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectBoundary'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.w3.org/2004/02/skos/core#Collection'(X),'http://www.w3.org/2004/02/skos/core#Concept'(X).
'http://www.example.org/fresh#fresh14'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SidetrackWellbore'(X).
'http://www.opengis.net/ont/gml#TriangulatedSurface'(X) :- 'http://www.opengis.net/ont/gml#Tin'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Equipment'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Rig'(X).
'http://www.opengis.net/ont/sf#Geometry'(X) :- 'http://www.opengis.net/ont/sf#Point'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellTarget'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MainArea'(X).
'http://www.opengis.net/ont/gml#Composite'(X) :- 'http://www.opengis.net/ont/gml#CompositeSurface'(X).
'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Point'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyCoordinate'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MopustorFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#RealizableEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Disposition'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BAAArea'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectBoundary'(X),'http://www.ifomis.org/bfo/1.1/snap#Object'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#SpecificallyDependentContinuant'(X),'http://www.ifomis.org/bfo/1.1/snap#GenericallyDependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#BAAArea'(X).
'http://www.opengis.net/ont/gml#AbstractSurfacePatch'(X) :- 'http://www.opengis.net/ont/gml#PolygonPatch'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Formation'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Formation'(X),'http://sws.ifi.uio.no/vocab/npd-v2#Group'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SuspReenteredLaterWellbore'(X).
'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X) :- 'http://www.opengis.net/ont/gml#Curve'(X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(X) :- 'http://www.opengis.net/ont/geosparql#Feature'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#AppraisalWell'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ParcellBAA'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X).
'http://www.opengis.net/ont/gml#BSpline'(X) :- 'http://www.opengis.net/ont/gml#Bezier'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MonotowerFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#AppraisalWellbore'(X).
'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#SpecificallyDependentContinuant'(X).
'http://www.opengis.net/ont/gml#OrientableCurve'(X) :- 'http://www.opengis.net/ont/gml#Curve'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://www.opengis.net/ont/geosparql#SpatialObject'(X).
'http://www.opengis.net/ont/sf#Surface'(X) :- 'http://www.opengis.net/ont/sf#PolyhedralSurface'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#LineStringSegment'(X).
'http://www.opengis.net/ont/gml#PolygonPatch'(X) :- 'http://www.opengis.net/ont/gml#Rectangle'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#GroundSurvey'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillStemTest'(X).
'http://www.ifomis.org/bfo/1.1/snap#Object'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Licensee'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicensee'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Site'(X),'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ConnectedTemporalRegion'(X),'http://www.ifomis.org/bfo/1.1/span#ScatteredTemporalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TlpConcreteFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#Notification'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryArea'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryArea'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#OilPipeline'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#GeodesicString'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Operator'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceOperator'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFNotification'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#RecoveryWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionWell'(X).
'http://www.example.org/fresh#fresh13'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X).
'http://www.opengis.net/ont/gml#AbstractSurfacePatch'(X) :- 'http://www.opengis.net/ont/gml#AbstractParametricCurveSurface'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellHead'(X).
'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ThreeDimensionalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultiFieldWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Licensee'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceLicensee'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualContext'(X),'http://www.ifomis.org/bfo/1.1/span#FiatProcessPart'(X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#Notification'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ChangedAwardNotification'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Point'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TerminationPoint'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalRegion'(X),'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessAggregate'(X),'http://www.ifomis.org/bfo/1.1/span#ProcessualContext'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X),'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultiWellTemplateFacility'(X).
'http://www.opengis.net/ont/gml#OrientableCurve'(X) :- 'http://www.opengis.net/ont/gml#CompositeCurve'(X).
'http://www.opengis.net/ont/gml#AbstractGriddedSurface'(X) :- 'http://www.opengis.net/ont/gml#Cylinder'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#InitialWellbore'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#NCSYearlyProduction'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceArea'(X).
'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Reserve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultilateralWell'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceArea'(X).
'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ContingentResources'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualContext'(X),'http://www.ifomis.org/bfo/1.1/span#Process'(X).
'http://www.opengis.net/ont/gml#LineStringSegment'(X) :- 'http://www.opengis.net/ont/gml#LineString'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TransportationPipeline'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#CondeepMonoshaftFacility'(X).
'http://www.opengis.net/ont/gml#Ring'(X) :- 'http://www.opengis.net/ont/gml#LinearRing'(X).
'http://www.opengis.net/ont/gml#GeodesicString'(X) :- 'http://www.opengis.net/ont/gml#Geodesic'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X).
'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Object'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#AwardArea'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#TemporalInstant'(X),'http://www.ifomis.org/bfo/1.1/span#TemporalInterval'(X).
'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X) :- 'http://www.opengis.net/ont/gml#OrientableCurve'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#FiatObjectPart'(X),'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Royalty'(X).
'http://www.w3.org/2004/02/skos/core#Concept'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_2'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Well'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ShallowWell'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualContext'(X),'http://www.ifomis.org/bfo/1.1/span#ProcessBoundary'(X).
'http://www.opengis.net/ont/sf#LineString'(X) :- 'http://www.opengis.net/ont/sf#LinearRing'(X).
'http://www.ifomis.org/bfo/1.1/snap#Site'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCasingAndLeakoffTest'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultifieldWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ConcreteStructureFacility'(X).
'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalRegion'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ThreeDimensionalRegion'(X),'http://www.ifomis.org/bfo/1.1/snap#OneDimensionalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Reserve'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryReserve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#MultilateralWellbore'(X).
'http://www.opengis.net/ont/gml#SplineCurve'(X) :- 'http://www.opengis.net/ont/gml#BSpline'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectBoundary'(X),'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X).
'http://www.ifomis.org/bfo/1.1/span#ConnectedSpatiotemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalInstant'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SurveyMultilineArea'(X).
'http://www.ifomis.org/bfo/1.1/snap#SpecificallyDependentContinuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#RealizableEntity'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SurveyMultilineArea'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Licensee'(X).
'http://www.opengis.net/ont/gml#Composite'(X) :- 'http://www.opengis.net/ont/gml#CompositeCurve'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldInvestment'(X).
'http://www.opengis.net/ont/geosparql#Geometry'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldArea'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldArea'(X).
'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#OneDimensionalRegion'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ThreeDimensionalRegion'(X),'http://www.ifomis.org/bfo/1.1/snap#TwoDimensionalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#Notification'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#EasementNotification'(X).
'http://www.ifomis.org/bfo/1.1/snap#Site'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Prospect'(X).
'http://www.opengis.net/ont/gml#ArcByCenterPoint'(X) :- 'http://www.opengis.net/ont/gml#CircleByCenterPoint'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SubseaStructureFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#Clothoid'(X).
'http://www.ifomis.org/bfo/1.1/span#ConnectedSpatiotemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalInterval'(X).
'http://www.opengis.net/ont/sf#Curve'(X) :- 'http://www.opengis.net/ont/sf#LineString'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Transfer'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X).
'http://www.opengis.net/ont/gml#AbstractGriddedSurface'(X) :- 'http://www.opengis.net/ont/gml#Sphere'(X).
'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X) :- 'http://www.opengis.net/ont/gml#Point'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Area'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LicencedAcreage'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Operator'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#CompressedNaturalGas'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ObservationWellbore'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FixedFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Sidetrack'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LiquefiedNaturalGas'(X).
'http://www.example.org/fresh#fresh1'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SubseaCompletedWell'(X).
'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#TwoDimensionalRegion'(X).
'http://www.example.org/fresh#fresh4'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#JackUp3LegsFacility'(X).
'http://www.ifomis.org/bfo/1.1/span#ProcessualEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ProcessualContext'(X).
'http://www.example.org/fresh#fresh10'(X) :- 'http://www.ifomis.org/bfo/1.1/span#Occurrent'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Operator'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#TUFOperator'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalInterval'(X),'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalInstant'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ZeroDimensionalRegion'(X),'http://www.ifomis.org/bfo/1.1/snap#ThreeDimensionalRegion'(X).
'http://www.opengis.net/ont/gml#Surface'(X) :- 'http://www.opengis.net/ont/gml#Polygon'(X).
'http://www.w3.org/2004/02/skos/core#ConceptScheme'(X) :- 'http://resource.geosciml.org/ontology/trs-30#TimeOrdinalReferenceSystem'(X).
'http://www.opengis.net/ont/sf#PolyhedralSurface'(X) :- 'http://www.opengis.net/ont/sf#TIN'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Agent'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DryGas'(X).
'http://www.w3.org/2004/02/skos/core#Concept'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_1'(X).
'http://www.example.org/fresh#fresh7'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#SpecificallyDependentContinuant'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Reserve'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#CompanyReserve'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#RecoveryWell'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#InjectionWell'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ElectromagneticSurvey'(X).
'http://www.opengis.net/ont/gml#OrientableSurface'(X) :- 'http://www.opengis.net/ont/gml#Surface'(X).
'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X) :- 'http://www.opengis.net/ont/gml#Surface'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Survey'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#OtherSurvey'(X).
'http://www.ifomis.org/bfo/1.1/snap#IndependentContinuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Field'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratigraphicCoreSet'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Site'(X),'http://www.ifomis.org/bfo/1.1/snap#FiatObjectPart'(X).
'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ZeroDimensionalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Jacket6LegsFacility'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#OilGasPipeline'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Facility'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Condeep3ShaftsFacility'(X).
'http://www.ifomis.org/bfo/1.1/snap#MaterialEntity'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#AreaFee'(X).
'http://www.w3.org/2004/02/skos/core#Concept'(X) :- 'http://www.example.org/fresh#_eliminatedtransfresh_3'(X).
'http://www.opengis.net/ont/gml#AbstractCurveSegment'(X) :- 'http://www.opengis.net/ont/gml#ArcByCenterPoint'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Object'(X),'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X).
'http://www.ifomis.org/bfo/1.1/snap#Continuant'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceNotification'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#Role'(X),'http://www.ifomis.org/bfo/1.1/snap#Function'(X).
'http://www.w3.org/2004/02/skos/core#Concept'(X) :- 'http://resource.geosciml.org/ontology/trs-30#TimeOrdinalEraBoundary'(X).
'http://www.ifomis.org/bfo/1.1/span#SpatiotemporalRegion'(X) :- 'http://www.ifomis.org/bfo/1.1/span#ConnectedSpatiotemporalRegion'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#TwoDimensionalRegion'(X),'http://www.ifomis.org/bfo/1.1/snap#OneDimensionalRegion'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Licensee'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#FieldLicensee'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#SeismicAreaBAA'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#AssociatedGas'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Gas'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WetGas'(X).
'http://www.opengis.net/ont/gml#AbstractGeometricPrimitive'(X) :- 'http://www.opengis.net/ont/gml#Solid'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#ObjectBoundary'(X),'http://www.ifomis.org/bfo/1.1/snap#Site'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#Plan'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#PlanForInstallationAndOperation'(X).
'http://www.opengis.net/ont/sf#GeometryCollection'(X) :- 'http://www.opengis.net/ont/sf#MultiSurface'(X).
'http://www.w3.org/2002/07/owl#Nothing'(X) :- 'http://www.ifomis.org/bfo/1.1/snap#DependentContinuant'(X),'http://www.ifomis.org/bfo/1.1/snap#SpatialRegion'(X).
'http://www.opengis.net/ont/gml#PolynomialSpline'(X) :- 'http://www.opengis.net/ont/gml#CubicSpline'(X).
'http://www.ifomis.org/bfo/1.1/snap#ObjectAggregate'(X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#PetroleumDeposit'(X).
'http://www.opengis.net/ont/sf#Geometry'(X) :- 'http://www.opengis.net/ont/sf#Curve'(X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehMeet'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#lastOperatorCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldInvestment'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#investmentForField'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeCompany'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8ntpp'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8tppi'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel2'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAATransfer'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#baaTransferCompany'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfIntersects'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LOTForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#includedInField'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellOperator'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#taskForCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coresForWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyCoordinate'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coordinateForSurvey'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coordinateForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#currentResponsibleCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillingMudSample'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#mudTestForWellbore'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehOverlap'(Y,X).
'http://www.w3.org/2004/02/skos/core#OrderedCollection'(Y) :- 'http://www.w3.org/2004/02/skos/core#memberList'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeForTUF'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#statusForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCorePhoto'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#corePhotoURL'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#messageForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#resourcesIncludedInDiscovery'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#taskForLicence'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehInside'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfEquals'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#AwardArea'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#provinceLocation'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8ec'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfDisjoint'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehEquals'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceStatus'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#statusForLicence'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8po'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForBAA'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#includedInField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForField'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceLicenceeCompany'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfOverlaps'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#baaTransferCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenceLicensee'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#CompanyReserve'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reservesForCompany'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_0'(Y) :- 'http://www.example.org/fresh#_eliminatedtransfresh_0'(X),'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreStratum'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#stratumForWellbore'(Y,X).
'http://resource.geosciml.org/ontology/timescale/gts-30#GeochronologicEra'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#geochronologicEra'(X,Y).
'http://www.opengis.net/ont/geosparql#Feature'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#baaOperatorCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reservesForCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#statusForSurvey'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel1'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehCovers'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFOperator'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorForLicence'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8dc'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#MoveableFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#registeredInCountry'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehMeet'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#FacilityPoint'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#belongsToFacility'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#fieldLicensee'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#discoveryWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ownerForField'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehDisjoint'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForField'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehOverlap'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8ec'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#taskForCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceOperator'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenceOperatorCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForField'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8po'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehDisjoint'(X,Y).
'http://www.w3.org/2004/02/skos/core#Collection'(Y) :- 'http://www.w3.org/2004/02/skos/core#member'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreOilSample'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#oilSampleTestForWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceTransfer'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenceTransferCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAATransfer'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#transferredBAA'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfContains'(Y,X).
'http://www.w3.org/2004/02/skos/core#Concept'(Y) :- 'http://www.w3.org/2004/02/skos/core#semanticRelation'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#transferredLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#pipelineFromFacility'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehEquals'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDocument'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#documentForWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reportingCompany'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfEquals'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#fieldOperator'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDocument'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#documentURL'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenceTransferCompany'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reclassedFromWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#BAALicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForBAA'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8eq'(X,Y).
'http://resource.geosciml.org/ontology/timescale/gts-30#GeochronologicEra'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHc'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForField'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#statusForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceOperatorCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#documentForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ShallowWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#shallowWellboreForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#discoveryWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#stratumForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldStatus'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#statusForField'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#operatorForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#fieldLicensee'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceOperator'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#operatorForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFNotification'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageForTUF'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceOperatorCompany'(X,Y).
'http://www.opengis.net/ont/geosparql#Geometry'(Y) :- 'http://www.opengis.net/ont/geosparql#hasGeometry'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#BAALicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#baaLicensee'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfCrosses'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coresForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Quadrant'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#quadrantLocation'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#productionFacility'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#pressReleaseURL'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#DevelopmentWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForField'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8ntppi'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForField'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Block'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#containsWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorForLicence'(X,Y).
'http://www.w3.org/2004/02/skos/core#Concept'(Y) :- 'http://www.w3.org/2004/02/skos/core#hasTopConcept'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCasingAndLeakoffTest'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#LOTForWellbore'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfTouches'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Group'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(X,Y),'http://sws.ifi.uio.no/vocab/npd-v2#Formation'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoordinate'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coordinateForWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Formation'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#Member'(X),'http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#taskForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DSTForWellbore'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8ntppi'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#messageForTUF'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOwner'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldOperator'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#fieldOperator'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCoreSet'(X),'http://sws.ifi.uio.no/vocab/npd-v2#member'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOperatorCompany'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfWithin'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfContains'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#DiscoveryReserve'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reservesForDiscovery'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFOwner'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceeForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#transferredBAA'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreCorePhoto'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#corePhotoForWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#operatorForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#AwardArea'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#npdPageURL'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_3'(Y) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y),'http://www.example.org/fresh#_eliminatedtransfresh_3'(X).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeTD'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#investmentForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reportingCompany'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehCoveredBy'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#pipelineOperator'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurveyProgress'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#statusForSurvey'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coreForWellbore'(X,Y).
'http://www.w3.org/2004/02/skos/core#ConceptScheme'(Y) :- 'http://www.w3.org/2004/02/skos/core#topConceptOf'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHc'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehContains'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOperator'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#MoveableFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#currentResponsibleCompany'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForDiscovery'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#pipelineOperator'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#corePhotoForWellbore'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#containsWellbore'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8eq'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#baaLicensee'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8tpp'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#drillingOperatorCompany'(X,Y).
'http://www.w3.org/1999/02/22-rdf-syntax-ns#List'(Y) :- 'http://www.w3.org/2004/02/skos/core#memberList'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForLicence'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_1'(Y) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8tppi'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8ntpp'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_2'(Y) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFLicenceeForTUF'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Wellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#mudTestForWellbore'(X,Y).
'http://www.opengis.net/ont/geosparql#Feature'(Y) :- 'http://www.opengis.net/ont/geosparql#defaultGeometry'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceLicenceeCompany'(X,Y).
'http://www.w3.org/2004/02/skos/core#ConceptScheme'(Y) :- 'http://www.w3.org/2004/02/skos/core#inScheme'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#WellboreDrillStemTest'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#DSTForWellbore'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceTransfer'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#transferredLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#belongsToFacility'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOperator'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfWithin'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Discovery'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reservesForDiscovery'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfIntersects'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_2'(Y) :- 'http://www.example.org/fresh#_eliminatedtransfresh_2'(X),'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFacility'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerForLicence'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfCrosses'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceLicensee'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#licenceeForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#TUFOwner'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2-ptl#TUFOwnerForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenceLicensee'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfDisjoint'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_3'(Y) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#reservesForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Pipeline'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#pipelineToFacility'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#BAA'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#baaOperatorCompany'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehInside'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfTouches'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ExplorationWellbore'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel3'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2-ptl#ProductionLicenceNotification'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#messageForLicence'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#SeismicSurvey'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#coordinateForSurvey'(X,Y).
'http://www.example.org/fresh#fresh8'(Y) :- 'http://www.w3.org/2004/02/skos/core#member'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldOperator'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#operatorForField'(Y,X).
'http://www.example.org/fresh#_eliminatedtransfresh_1'(Y) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y),'http://www.example.org/fresh#_eliminatedtransfresh_1'(X).
'http://www.example.org/fresh#_eliminatedtransfresh_0'(Y) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#Company'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenceOperatorCompany'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8dc'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#shallowWellboreForLicence'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#sfOverlaps'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehCoveredBy'(Y,X).
'http://sws.ifi.uio.no/vocab/npd-v2#Field'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#currentFieldOwner'(Y,X).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#rcc8tpp'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#FieldOwner'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#ownerForField'(Y,X).
'http://www.w3.org/2004/02/skos/core#Concept'(Y) :- 'http://www.w3.org/2004/02/skos/core#semanticRelation'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehCovers'(X,Y).
'http://www.opengis.net/ont/geosparql#SpatialObject'(Y) :- 'http://www.opengis.net/ont/geosparql#ehContains'(X,Y).
'http://www.opengis.net/ont/geosparql#Geometry'(Y) :- 'http://www.opengis.net/ont/geosparql#defaultGeometry'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicence'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#licenseeForLicence'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#LithostratigraphicUnit'(Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#stratigraphicParent'(X,Y).
'http://www.w3.org/2004/02/skos/core#closeMatch'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#closeMatch'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHc'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel1'(X,Y).
'http://www.opengis.net/ont/geosparql#hasGeometry'(X,Y) :- 'http://www.opengis.net/ont/geosparql#defaultGeometry'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForField'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#explorationWellboreForField'(X,Y).
'http://www.w3.org/2004/02/skos/core#mappingRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#broadMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#semanticRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#mappingRelation'(X,Y).
'http://www.w3.org/2004/02/skos/core#exactMatch'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#exactMatch'(Y,X).
'http://www.w3.org/2004/02/skos/core#inScheme'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#topConceptOf'(X,Y).
'http://www.w3.org/2004/02/skos/core#closeMatch'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#closeMatch'(Y,X).
'http://www.w3.org/2004/02/skos/core#semanticRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y).
'http://www.w3.org/2004/02/skos/core#mappingRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#relatedMatch'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHc'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel3'(X,Y).
'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#narrower'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHc'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel2'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHc'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel2'(X,Y).
'http://www.w3.org/2004/02/skos/core#related'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#related'(X,Y).
'http://www.w3.org/2004/02/skos/core#mappingRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#closeMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#related'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#relatedMatch'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHc'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreFormationHcLevel1'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHc'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#wellboreAgeHcLevel3'(X,Y).
'http://www.w3.org/2004/02/skos/core#semanticRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y).
'http://www.w3.org/2004/02/skos/core#related'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#related'(Y,X).
'http://www.w3.org/2004/02/skos/core#inScheme'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#hasTopConcept'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForField'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#shallowWellboreForField'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#wellboreForField'(X,Y) :- 'http://sws.ifi.uio.no/vocab/npd-v2#developmentWellboreForField'(X,Y).
'http://www.w3.org/2004/02/skos/core#narrower'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#narrowMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#exactMatch'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#exactMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#closeMatch'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#exactMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#semanticRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#related'(X,Y).
'http://www.w3.org/2004/02/skos/core#mappingRelation'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#narrowMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#broader'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#broadMatch'(X,Y).
'http://www.w3.org/2004/02/skos/core#semanticRelation'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y).
'http://www.w3.org/2004/02/skos/core#relatedMatch'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#relatedMatch'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(X,Y) :- 'http://www.opengis.net/ont/geosparql#defaultGeometry'(Y,X).
'http://www.w3.org/2004/02/skos/core#relatedMatch'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#relatedMatch'(Y,X).
'http://www.w3.org/2004/02/skos/core#semanticRelation'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y).
'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#broader'(X,Y).
'http://www.w3.org/2004/02/skos/core#broadMatch'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#narrowMatch'(Y,X).
'http://www.w3.org/2004/02/skos/core#narrowMatch'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#broadMatch'(X,Y).
'http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(X,Y) :- 'http://www.opengis.net/ont/geosparql#hasGeometry'(Y,X).
'http://www.opengis.net/ont/geosparql#hasGeometry'(Y,X) :- 'http://sws.ifi.uio.no/vocab/npd-v2#isGeometryOfFeature'(X,Y).
'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(Y,X).
'http://www.w3.org/2004/02/skos/core#broaderTransitive'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y).
'http://www.w3.org/2004/02/skos/core#broader'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#narrower'(Y,X).
'http://www.w3.org/2004/02/skos/core#narrower'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#broader'(X,Y).
'http://www.w3.org/2004/02/skos/core#hasTopConcept'(X,Y) :- 'http://www.w3.org/2004/02/skos/core#topConceptOf'(Y,X).
'http://www.w3.org/2004/02/skos/core#topConceptOf'(Y,X) :- 'http://www.w3.org/2004/02/skos/core#hasTopConcept'(X,Y).
'http://www.w3.org/2004/02/skos/core#exactMatch'(X,Z) :- 'http://www.w3.org/2004/02/skos/core#exactMatch'(X,Y),'http://www.w3.org/2004/02/skos/core#exactMatch'(Y,Z).
'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Z) :- 'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(X,Y),'http://www.w3.org/2004/02/skos/core#narrowerTransitive'(Y,Z).
'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Z) :- 'http://www.w3.org/2004/02/skos/core#broaderTransitive'(X,Y),'http://www.w3.org/2004/02/skos/core#broaderTransitive'(Y,Z).
