from rdflib import Graph
import time
import glob
import os
# import multiprocessing
import re


NUMBER_PATTERN = "\d+"
NS = "http://semantics.crl.ibm.com/univ-bench-dl.owl#"


def parse_triple_file(rdf_file):
    print "Current file is: " + rdf_file
    outfile = rdf_file + '.sql'
    # check if file exists for resume purpose
    # try:
    #     with open(outfile):
    #         print "File already written, does nothing. Bye!"
    #         return
    # except:
    #     pass

    f = open(outfile, "w")
    print >> f, "USE uobm;"

    graph = Graph()
    print "Start loading the file"
    graph.parse(rdf_file, format="nt")

    # NOTE: how to convert from RDF/XML to N-triples
    # graph.parse(triple_file, format="xml")
    # print "Now serializing it"
    # graph.serialize(triple_file + ".nt", format="nt")

    insert_universities(graph, f)
    insert_colleges(graph, f)
    insert_departments(graph, f)
    insert_research_groups(graph, f)
    insert_courses(graph, f)
    insert_persons(graph, f)
    insert_professors(graph, f)
    insert_lecturers(graph, f)
    insert_graduate_students(graph, f)
    insert_undergraduate_students(graph, f)
    insert_teaching_assistants(graph, f)
    insert_teaching(graph, f)
    insert_course_enrollments(graph, f)

    insert_chairs(graph, f)
    insert_support_staff(graph, f)
    insert_friends(graph, f)
    insert_interests(graph, f)
    insert_publications(graph, f)
    insert_authors(graph, f)
    f.close()


def insert_authors(graph, f):
    print "Insert authors"
    author_query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?pub ?author
        WHERE {
            ?pub :publicationAuthor ?author .
        }
    """
    for r in graph.query(author_query):
        pub_id, pub_dep_id, pub_uni_id, _ = extract_ids(r["pub"])
        author_id, author_dep_id, author_uni_id, author_role = extract_ids(r["author"])

        # Assumptions
        assert(pub_dep_id == author_dep_id)
        assert(pub_uni_id == author_uni_id)

        print >> f, "INSERT INTO Authors VALUES (%s, %s, %s, '%s', %s);" % (
            author_id, author_dep_id, author_uni_id, author_role, pub_id
        )


def insert_chairs(graph, f):
    print "Insert chairs"
    chair_query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?chair
    WHERE {
       ?chair a :Chair .
    }
    """
    for r in graph.query(chair_query):
        chair_id, dep_id,  uni_id, _ = extract_ids(r["chair"])
        print >> f, "INSERT INTO Chairs VALUES (%s, %s, %s);" % (
            chair_id, dep_id, uni_id
        )


def insert_colleges(graph, f):
    """
    We don't care about woman college for the moment.
    """
    print "Insert colleges"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?college ?uni
    WHERE {
       ?college a :College ;
            :subOrganizationOf ?uni .
    }
    """
    for r in graph.query(query):
        college_id,  uni_id = extract_numbers(r["college"])
        print >> f, "INSERT INTO Colleges VALUES (%s, %s);" % (
            uni_id, college_id
        )


def insert_course_enrollments(graph, f):
    print "Insert course enrollments"
    query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?student ?course
        WHERE {
           ?student :takesCourse ?course.
        }"""
    for r in graph.query(query):
        course_id, course_dep_id, course_uni_id, course_type = extract_ids(r["course"])
        stud_id, stud_dep_id,  stud_uni_id, student_type = extract_ids(r["student"])

        print >> f, "INSERT INTO CourseEnrollments VALUES (%s, %s, %s, '%s', %s, %s, %s, '%s');" % (
            course_id, course_dep_id, course_uni_id, course_type,
            stud_id, stud_dep_id, stud_uni_id, student_type
        )


def insert_courses(graph, f):
    """
        We do not distinguish graduate courses for the moment
        (only encoded in the URIs in the RDF graph)x
    """
    print "Insert courses"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?course ?name
    WHERE {
       ?course a :Course ;
            :name ?name .
    }
    """
    for r in graph.query(query):
        course_id, dep_id,  uni_id, course_type = extract_ids(r["course"])
        # Fix bug in the RDF graphs (object property instead of litteral)
        # course_name = r["name"]
        course_name = str(r["name"]).split("/")[-1]

        print >> f, "INSERT INTO Courses VALUES (%s, %s, %s, '%s', '%s');" % (
            course_id, dep_id, uni_id, course_type, course_name
        )


def insert_departments(graph, f):
    print "Insert departments"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?dep ?college ?name
    WHERE {
       ?dep a :Department ;
            :subOrganizationOf ?college ;
            :name ?name .
    }
    """
    for r in graph.query(query):
        dep_id,  uni_id = extract_numbers(r["dep"])
        college_id = extract_numbers(r["college"])[0]
        # BUG in the generated RDF file
        # dep_name = r["name"]
        dep_name = "Department"+dep_id
        print >> f, "INSERT INTO Departments VALUES (%s, %s, %s, '%s');" % (
            dep_id, uni_id, college_id, dep_name
        )


def insert_friends(graph, f):
    print "Insert friendships"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?person ?friend
    WHERE {
       ?person :isFriendOf ?friend .
    }
    """
    for r in graph.query(query):
        person_id, person_dep_id,  person_uni_id, person_role = extract_ids(r["person"])
        friend_id, friend_dep_id, friend_uni_id, friend_role = extract_ids(r["friend"])

        print >> f, "INSERT INTO Friends VALUES (%s, %s, %s, '%s', %s, %s, %s, '%s');" % (
            friend_id, friend_dep_id, friend_uni_id, friend_role,
            person_id, person_dep_id,  person_uni_id, person_role
        )


def insert_graduate_students(graph, f):
    print "Insert graduate students"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?student ?u_uni ?major ?isAssistant ?advisor
    WHERE {
       ?student a :GraduateStudent ;
            :hasUndergraduateDegreeFrom ?u_uni ;
            :hasMajor ?major ;
            :isAdvisedBy ?advisor .

        OPTIONAL {
            ?student a :ResearchAssistant .
            BIND(true as ?isAssistant)
       }
    }
    """

    for r in graph.query(query):
        stud_id, dep_id,  uni_id, _ = extract_ids(r["student"])
        u_uni_id = extract_numbers(r["u_uni"])[-1]
        is_r_assistant = "1" if r["isAssistant"] is not None else "0"
        advisor_id, advisor_dep_id, advisor_uni_id, advisor_position = extract_ids(r["advisor"])

        # Our assumptions
        assert(dep_id == advisor_dep_id)
        assert(uni_id == advisor_uni_id)

        print >> f, "INSERT INTO GraduateStudents VALUES (%s, %s, %s, %s, '%s', %s, '%s', %s);" % (
            stud_id, uni_id, dep_id, advisor_id, advisor_position, u_uni_id, r["major"], is_r_assistant
        )


def insert_interests(graph, f):
    print "Insert interests"
    normal_interest_query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?person ?interest
    WHERE {
        ?person ?p ?interest .

        VALUES (?p) {
            (:like)
            (:love)
        }
    }"""

    for r in graph.query(normal_interest_query):
        person_id, dep_id, uni_id, role = extract_ids(r["person"])
        print >> f,  "INSERT INTO Interests VALUES (%s, %s, %s, '%s', '%s', 0);" % (
            person_id, dep_id, uni_id, role, r["interest"]
        )

    crazy_interest_query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?person ?interest
    WHERE {
        ?person :isCrazyAbout ?interest .
    }"""

    for r in graph.query(crazy_interest_query):
        person_id, dep_id, uni_id, role = extract_ids(r["person"])
        print >> f,  "INSERT INTO Interests VALUES (%s, %s, %s, '%s', '%s', 1);" % (
            person_id, dep_id, uni_id, role, r["interest"]
        )


def insert_lecturers(graph, f):
    """TODO: see if should be merged with professors """
    print "Insert lecturers"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?lecturer ?u_uni ?m_uni ?d_uni ?dep
    WHERE {
       ?lecturer a :Lecturer ;
            :hasUndergraduateDegreeFrom ?u_uni ;
            :hasMasterDegreeFrom ?m_uni ;
            :hasDoctoralDegreeFrom ?d_uni .

        OPTIONAL {
            ?lecturer :worksFor ?dep .
        }
    }
    """
    for r in graph.query(query):
        lecturer_uri = r["lecturer"]
        dep_id,  uni_id, lecturer_id = extract_numbers(lecturer_uri)
        u_uni_id = extract_numbers(r["u_uni"])[-1]
        m_uni_id = extract_numbers(r["m_uni"])[-1]
        d_uni_id = extract_numbers(r["d_uni"])[-1]
        is_working = "1" if r["dep"] is not None else "0"

        print >> f, "INSERT INTO Lecturers VALUES (%s, %s, %s, %s, %s, %s, %s);" % (
            lecturer_id, dep_id, uni_id, u_uni_id, m_uni_id, d_uni_id, is_working
        )


def insert_persons(graph, f):
    print "Insert persons"
    person_partial_query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?p ?firstName ?lastName ?email ?telephone ?gender
        WHERE {
            ?p a %s ;
              :firstName ?firstName ;
              :lastName ?lastName ;
              :telephone ?telephone ;
              :emailAddress ?email .
        }"""

    for cls, gender in [(":Woman", "F"), (":Man", "M")]:
        for r in graph.query(person_partial_query % cls):
            person_id, dep_id, uni_id, person_role = extract_ids(r["p"])

            print >> f, "INSERT INTO People VALUES (%s, %s, %s, '%s', '%s', '%s', '%s', '%s', '%s');" % (
                person_id, dep_id, uni_id, person_role, r["firstName"],
                r["lastName"], r["email"], r["telephone"], gender)


def insert_professors(graph, f):
    """ Here we only consider EXPLICIT RDF graphs.

        :isHeadOf implies :worksFor during we currently ignore this rule.
    """
    print "Insert professors"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?prof ?u_uni ?m_uni ?d_uni ?interest ?dep ?position ?leadDep
    WHERE {
       ?prof a ?position ;
            :hasUndergraduateDegreeFrom ?u_uni ;
            :hasMasterDegreeFrom ?m_uni ;
            :hasDoctoralDegreeFrom ?d_uni ;
            :researchInterest ?interest .

        OPTIONAL {
            ?prof :worksFor ?dep .
        }
        OPTIONAL {
            ?prof :isHeadOf ?leadDep .
        }

        VALUES (?position) {
            (:FullProfessor)
            (:AssociateProfessor)
            (:AssistantProfessor)
        }
    }
    """

    for r in graph.query(query):
        prof_id, dep_id, uni_id, role = extract_ids(r["prof"])
        interest_id = extract_numbers(r["interest"])[-1]
        u_uni_id = extract_numbers(r["u_uni"])[-1]
        m_uni_id = extract_numbers(r["m_uni"])[-1]
        d_uni_id = extract_numbers(r["d_uni"])[-1]
        is_working = "1" if r["dep"] is not None else "0"
        position = extract_class_name(r["position"])
        assert(position == role)
        is_head = "1" if r["leadDep"] is not None else "0"

        print >> f, "INSERT INTO Professors VALUES (%s, %s, %s, '%s', %s, %s, %s, %s, %s, %s);" % (
            prof_id, dep_id, uni_id, role, interest_id, u_uni_id, m_uni_id, d_uni_id,
            is_working, is_head
        )


def insert_publications(graph, f):
    print "Insert publications"
    publication_query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?pub ?pubType
        WHERE {
            ?pub :publicationAuthor ?author ;
                a ?pubType .
        }"""
    for r in graph.query(publication_query):
        dep_id, uni_id, pub_id = extract_numbers(r["pub"])
        pub_type = extract_class_name(r["pubType"])
        print >> f, "INSERT INTO Publications VALUES (%s, %s, %s, '%s');" % (
            pub_id, dep_id, uni_id, pub_type
        )


def insert_research_groups(graph, f):
    print "Insert research groups"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?group
    WHERE {
       ?group a :ResearchGroup .
    }
    """
    for r in graph.query(query):
        group_id, dep_id,  uni_id, _ = extract_ids(r["group"])
        print >> f, "INSERT INTO ResearchGroups VALUES (%s, %s, %s);" % (
            group_id, dep_id, uni_id
        )


def insert_support_staff(graph, f):
    print "Insert clerical staff"
    partial_query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?employee
        WHERE {
            ?employee a ?position .
            VALUES  (?position) {
              (:ClericalStaff)
              (:SystemsStaff)
            }
        }"""
    for r in graph.query(partial_query):
        staff_id, dep_id, uni_id, role = extract_ids(r["employee"])

        print >> f, "INSERT INTO SupportStaff VALUES (%s, %s, %s, '%s');" % (
            staff_id, dep_id, uni_id, role
        )


def insert_teaching(graph, f):
    print "Insert teachings"
    query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?teacher ?course
        WHERE {
           ?teacher :teacherOf ?course .
        }"""
    for r in graph.query(query):
        teacher_uri = r["teacher"]
        dep_id,  uni_id, teacher_id = extract_numbers(teacher_uri)
        position = teacher_uri.split('/')[-1][:-len(teacher_id)]

        course_uri = r["course"]
        course_id = extract_numbers(course_uri)[-1]
        course_type = course_uri.split('/')[-1][:-len(course_id)]
        print >> f, "INSERT INTO Teaching VALUES (%s, '%s', %s, '%s', %s, %s);" % (
            course_id, course_type, teacher_id, position, uni_id, dep_id
        )


def insert_teaching_assistants(graph, f):
    print "Insert teaching assistants"
    query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?ta ?course
        WHERE {
           ?ta :teachingAssistantOf ?course .
        }"""
    for r in graph.query(query):
        dep_id,  uni_id, stud_id = extract_numbers(r["ta"])
        course_id = extract_numbers(r["course"])[-1]
        print >> f, "INSERT INTO TeachingAssistants VALUES (%s, %s, %s, %s);" % (
            stud_id, uni_id, dep_id, course_id
        )


def insert_undergraduate_students(graph, f):
    print "Insert undergraduate students"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?student ?major
    WHERE {
       ?student a :UndergraduateStudent ;
            :hasMajor ?major .
    }
    """

    for r in graph.query(query):
        stud_id, dep_id,  uni_id, _ = extract_ids(r["student"])
        print >> f, "INSERT INTO UnderGradStudents VALUES (%s, %s, %s, '%s');" % (
            stud_id, uni_id, dep_id, r["major"]
        )


def insert_universities(graph, f):
    print "Insert universities"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?uni ?name
    WHERE {
       ?uni a :University ;
            :name ?name .
    }
    """
    for r in graph.query(query):
        uni_id = extract_numbers(r["uni"])[0]
        print >> f, "INSERT INTO Universities VALUES (%s, '%s');" % (
            uni_id, r["name"]
        )


def extract_numbers(name):
    return tuple(re.findall(NUMBER_PATTERN, name))


def extract_class_name(class_uri):
    return class_uri[len(NS):]


def extract_individual_role(individual_uri, id):
    return individual_uri.split('/')[-1][:-len(id)]


def extract_ids(resource_uri):
    """ Works for persons (except college women) and courses """
    dep_id, uni_id, id = extract_numbers(resource_uri)
    role = extract_individual_role(resource_uri, id)
    return id, dep_id, uni_id, role


if __name__ == '__main__':
    #po = Pool()
    start_time = time.time()
    #jobs = []
    for triple_file in glob.glob(os.path.join('.', '*.nt')):
        # p = multiprocessing.Process(target=parsefile, args=(infile,))
        # jobs.append(p)
        # p.start()
        parse_triple_file(triple_file)

    # jobs[0].join()
    print "Time elapsed: ", time.time() - start_time, "s"