from rdflib import Graph
import time
import glob
import os
#import multiprocessing
import re


NUMBER_PATTERN = "\d+"
NS = "http://semantics.crl.ibm.com/univ-bench-dl.owl#"


def parseTripleFile(triple_file):
    print "Current file is: " + triple_file
    outfile = triple_file + '.sql'
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
    graph.parse(triple_file, format="nt")

    # NOTE: how to convert from RDF/XML to N-triples
    # graph.parse(triple_file, format="xml")
    # print "Now serializing it"
    # graph.serialize(triple_file + ".nt", format="nt")

    # insert_universities(graph, f)
    # insert_colleges(graph, f)
    # insert_departments(graph, f)
    # insert_courses(graph, f)
    # insert_persons(graph, f)
    # insert_professors(graph, f)
    insert_lecturers(graph, f)

    # insert_chairs(graph, f)
    # insert_support_staff(graph, f)
    # insert_publications(graph, f)
    # insert_authors(graph, f)
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
        dep_id, uni_id, pub_id = extract_numbers(r["pub"])

        print >> f, "INSERT INTO Authors VALUES (%s, %s, %s, '%s');" % (
            pub_id, dep_id, uni_id, r["author"]
        )


def insert_chairs(graph, f):
    print "Insert chairs"
    chair_query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?chair ?dep
    WHERE {
       ?chair a :Chair ;
            :isHeadOf ?dep .
    }
    """
    for r in graph.query(chair_query):
        dep_id,  uni_id = extract_numbers(r["dep"])
        print >> f, "INSERT INTO Chairs VALUES (%s, %s, '%s');" % (
            dep_id, uni_id, r["chair"]
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
        dep_id,  uni_id, course_id = extract_numbers(r["course"])
        # Fix bug in the RDF graphs (object property instead of litteral)
        # course_name = r["name"]
        course_name = str(r["name"]).split("/")[-1]
        course_level = 'G' if "Graduate" in course_name else 'U'

        print >> f, "INSERT INTO Courses VALUES (%s, %s, %s, '%s', '%s');" % (
            course_id, dep_id, uni_id, course_level, course_name
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
            ?lecturer :isMemberOf ?dep .
        }
    }
    """
    for r in graph.query(query):
        lecturer_uri = r["lecturer"]
        dep_id,  uni_id, lecturer_id = extract_numbers(lecturer_uri)
        u_uni_id = extract_numbers(r["u_uni"])[-1]
        m_uni_id = extract_numbers(r["m_uni"])[-1]
        d_uni_id = extract_numbers(r["d_uni"])[-1]
        is_member_dep = "1" if r["dep"] is not None else "0"

        print >> f, "INSERT INTO Lecturers VALUES (%s, %s, %s, %s, %s, %s, '%s', %s);" % (
            lecturer_id, dep_id, uni_id, u_uni_id, m_uni_id, d_uni_id,
            lecturer_uri, is_member_dep
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
            print >> f, "INSERT INTO People VALUES ('%s', '%s', '%s', '%s', '%s', '%s');" % (
                    r["p"], r["firstName"], r["lastName"], r["email"], r["telephone"], gender)


def insert_professors(graph, f):
    print "Insert professors"
    query = """
    PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
    SELECT DISTINCT ?prof ?u_uni ?m_uni ?d_uni ?interest ?dep ?position
    WHERE {
       ?prof a ?position ;
            :hasUndergraduateDegreeFrom ?u_uni ;
            :hasMasterDegreeFrom ?m_uni ;
            :hasDoctoralDegreeFrom ?d_uni ;
            :researchInterest ?interest .

        OPTIONAL {
            ?prof :isMemberOf ?dep .
        }
        VALUES (?position) {
            (:FullProfessor)
            (:AssociateProfessor)
            (:AssistantProfessor)
        }
    }
    """
    for r in graph.query(query):
        prof_uri = r["prof"]
        dep_id,  uni_id, prof_id = extract_numbers(prof_uri)
        interest_id = extract_numbers(r["interest"])[-1]
        u_uni_id = extract_numbers(r["u_uni"])[-1]
        m_uni_id = extract_numbers(r["m_uni"])[-1]
        d_uni_id = extract_numbers(r["d_uni"])[-1]
        is_member_dep = "1" if r["dep"] is not None else "0"
        position = extract_class_name(r["position"])

        print >> f, "INSERT INTO Professors VALUES (%s, '%s', %s, %s, %s, %s, %s, %s, '%s', %s);" % (
            prof_id, position, dep_id, uni_id, interest_id, u_uni_id, m_uni_id, d_uni_id,
            prof_uri, is_member_dep
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


def insert_support_staff(graph, f):
    print "Insert clerical staff"
    partial_query = """
        PREFIX : <http://semantics.crl.ibm.com/univ-bench-dl.owl#>
        SELECT DISTINCT ?employee ?position ?dep
        WHERE {
            ?employee a ?position ;
               :worksFor ?dep .
            VALUES  (?position) {
              (:ClericalStaff)
              (:SystemsStaff)
            }
        }"""
    for r in graph.query(partial_query):
        employee_uri = r["employee"]
        staff_id = extract_numbers(employee_uri)[2]
        dep_id, uni_id = extract_numbers(r["dep"])
        staff_type = extract_class_name(r["position"])

        print >> f, "INSERT INTO SupportStaff VALUES (%s, %s, %s, '%s', '%s');" % (
            staff_id, dep_id, uni_id, employee_uri, staff_type
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


def extract_class_name(uri):
    return uri[len(NS):]


if __name__ == '__main__':
    #po = Pool()
    start_time = time.time()
    #jobs = []
    for triple_file in glob.glob(os.path.join('.', '*.nt')):
        # p = multiprocessing.Process(target=parsefile, args=(infile,))
        # jobs.append(p)
        # p.start()
        parseTripleFile(triple_file)

    # jobs[0].join()
    print "Time elapsed: ", time.time() - start_time, "s"