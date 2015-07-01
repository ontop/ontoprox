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


optimized_expand(_, call(_), _, _) :- !, fail. % critical !!!

optimized_expand(_, Goal, Goal, _) :- edb(Goal), !. % EDB predicate should not be expanded. They will be defined by the mappings

optimized_expand(_, Goal, Goal, 0) :- !, fail. % this means you reach an IDB predicate, which should not be expanded further

optimized_expand(OriginalGoal, (Goal1,Goal2), (Expansion1, Expansion2), Depth) :- 
    optimized_expand(OriginalGoal, Goal1, Expansion1, Depth), optimized_expand(OriginalGoal, Goal2, Expansion2, Depth).

optimized_expand(OriginalGoal, Goal, Expansion, Depth) :-
     clause(Goal, Body),  not(member(OriginalGoal, Body)), Depth >= 1, Depth_1 is Depth - 1, optimized_expand(OriginalGoal, Body, Expansion, Depth_1).


%expand_list(Goal, GoalAndExpansion, Depth) :- expand(Goal, Expansion, Depth), 
%    flatten(Expansion, ExpansionList), GoalAndExpansion = (Goal, ExpansionList).
expand_list(Goal, GoalAndExpansion, Depth) :- optimized_expand(Goal, Goal, Expansion, Depth),
    flatten(Expansion, ExpansionList), GoalAndExpansion = (Goal, ExpansionList).

flatten(T, [T]) :- var(T), !.
flatten(T, [T]) :- atomic(T), !.
flatten((T1, T2), L) :- flatten(T1, L1), flatten(T2, L2), append(L1, L2, L), !.
flatten(T, [T]) :- compound(T), !.


%% remove redundant equivalent expansions

member(X, (X,_)) :- !.
member(X, X) :- !.
member(X,(_,T)) :- member(X,T).

memberEq(X,[Y|_]) :- X =@= Y, !.
memberEq(X,[_|T]) :- memberEq(X,T).

remove_equivalent([],[]).
remove_equivalent([H|T],[H|Out]) :-
    not(memberEq(H,T)),
    remove_equivalent(T,Out).
remove_equivalent([H|T],Out) :-
    memberEq(H,T),
    remove_equivalent(T,Out).

datalog_expansions(P, Depth, Expansions) :-
    findall(Expansion, expand_list(P, Expansion, Depth), IntermediateExpansions),
    remove_equivalent(IntermediateExpansions, Expansions).
    
print([H | T]) :- write_term(H, []), nl, print(T).
print([]) :- nl.  





rdfs_subsumes((A,B)) :- clause(A, B), not(B = (_,_)), not(edb(B)).   % A :- B 

rdfs_subsumes((A,B)) :- clause(A, C), not(C = (_,_)), not(edb(C)), rdfs_subsumes((C,B)). 




edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_College(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_University(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_emailAddress(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_firstName(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_lastName(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_like(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_name(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_researchInterest(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_telephone(_,_)).
edb(v_http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(_,_)).

http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Article(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_AssistantProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_AssociateProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Book(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Chair(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ClericalStaff(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_College(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ConferencePaper(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Course(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Department(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_FullProfessor(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_GraduateStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_JournalArticle(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Lecturer(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Man(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Manual(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Person(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchAssistant(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_ResearchGroup(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Software(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Specification(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_SystemsStaff(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_TeachingAssistant(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_TechnicalReport(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_UndergraduateStudent(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_University(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_University(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_UnofficialPublication(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(X) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_Woman(X).
http___uob_iodt_ibm_com_univ_bench_dl_owl_emailAddress(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_emailAddress(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_enrollIn(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_firstName(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_firstName(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasDoctoralDegreeFrom(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMajor(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasMasterDegreeFrom(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_hasUndergraduateDegreeFrom(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isAdvisedBy(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isCrazyAbout(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isFriendOf(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isHeadOf(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isMemberOf(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_isTaughtBy(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_lastName(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_lastName(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_like(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_name(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_name(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_publicationAuthor(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_researchInterest(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_researchInterest(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_takesCourse(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_teachingAssistantOf(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_telephone(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_telephone(X, Y).
http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(X, Y) :- v_http___uob_iodt_ibm_com_univ_bench_dl_owl_worksFor(X, Y).


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
