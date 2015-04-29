import xml.dom.minidom
import sys, os, glob, time, string
import multiprocessing
import re

NUMBER_PATTERN = "\d+"

PROF_CLASS = 'benchmark-dl:FullProfessor'


def file_exists(filename):
    try:
        with open(filename):
            return True
    except:
        return False


def extract_first_number(name):
    return re.search(NUMBER_PATTERN, name).group(0)


def parsefile(infile):
    filename = str(infile)
    print "Current file is: " + infile
    outfile = infile + '.sql'
    # check if file exists for resume purpose
    try:
        with open(outfile):
            return
    except:
        pass

    f = open(outfile, "w")
    print >> f, "USE uobm;"
    inf = open(infile, "r")
    indata = inf.read()
    inf.close()
    xmldoc = xml.dom.minidom.parseString(indata)

    # University 
    uni = xmldoc.getElementsByTagName('benchmark-dl:University')[0]

    uni_uri = uni.attributes['rdf:about'].value
    uni_name = uni.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    uniid = extract_first_number(uni_name)
    print >> f, "INSERT INTO Universities VALUES (%s, '%s');" % (uniid, uni_name)

    #dep = xmldoc.getElementsByTagName('benchmark-dl:Department')[0].attributes['rdf:about'].value
    #print >> f, "INSERT INTO departments VALUES (%s, %s);" % (depid, uniid)

    full_prof_list = xmldoc.getElementsByTagName(PROF_CLASS)
    # assoc_prof_list = xmldoc.getElementsByTagName('benchmark-dl:AssociateProfessor')
    # assist_prof_list = xmldoc.getElementsByTagName('benchmark-dl:AssistantProfessor')
    # lect_list = xmldoc.getElementsByTagName('benchmark-dl:Lecturer')
    #
    # under_stud_list = xmldoc.getElementsByTagName('benchmark-dl:UndergraduateStudent')
    # grad_stud_list = xmldoc.getElementsByTagName('benchmark-dl:GraduateStudent')
    #
    # research_list = xmldoc.getElementsByTagName('benchmark-dl:ResearchGroup')
    # pub_list = xmldoc.getElementsByTagName('benchmark-dl:Publication')
    #
    # ta_list = xmldoc.getElementsByTagName('benchmark-dl:TeachingAssistant')
    # ra_list = xmldoc.getElementsByTagName('benchmark-dl:ResearchAssistant')

    # full professors
    # for prof in full_prof_list:
    #     nameuri = prof.attributes['rdf:about'].value
    #     first_name = prof.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    #     tid = first_name.split('r')[2]
    #     # degrees
    #     under_d = prof.getElementsByTagName('benchmark-dl:University')[0].attributes['rdf:about'].value
    #     under = under_d.split('.')[1].split('y')[1]
    #     grad_d = prof.getElementsByTagName('benchmark-dl:University')[1].attributes['rdf:about'].value
    #     grad = grad_d.split('.')[1].split('y')[1]
    #     doc_d = prof.getElementsByTagName('benchmark-dl:University')[2].attributes['rdf:about'].value
    #     doc = doc_d.split('.')[1].split('y')[1]
    #     # personal info
    #     email = prof.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
    #     phone = prof.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
    #     res_int = prof.getElementsByTagName('benchmark-dl:researchInterest')[0].childNodes[0].nodeValue
    #     res = res_int.split('h')[1]
    #     print >> f, "INSERT INTO Professors VALUES (%s, %s, %s, %s, '%s', %s, %s, %s, '%s', '%s', %s);" % (
    #         depid, uniid, 3, tid, first_name, under, grad, doc, email, phone, res)
    #     # courses
    #     course_list = prof.getElementsByTagName('benchmark-dl:teacherOf')
    #     for course in course_list:
    #         c = course.attributes['rdf:resource'].value
    #         cname = c.split('/')[3]
    #         if cname[0] == 'C':
    #             ctype = 0
    #             cid = cname.split('e')[1]
    #         else:
    #             ctype = 1
    #             cid = cname.split('e')[2]
    #         print >> f, "INSERT INTO courses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, ctype, cid, tid, 3)

    # TODO: related to deans?
    #head = xmldoc.getElementsByTagName('benchmark-dl:headOf')[0].parentNode.attributes['rdf:about'].value
    #print >> f, "INSERT INTO heads VALUES (%s, %s, %s, %s);" % (depid, uniid, 3, head.split('r')[4])

    # # assoc professors
    # for prof in assoc_prof_list :
    # nameuri = prof.attributes['rdf:about'].value
    #     name = prof.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    #     tid = name.split('r')[2]
    #     # degrees
    #     under_d = prof.getElementsByTagName('benchmark-dl:University')[0].attributes['rdf:about'].value
    #     under = under_d.split('.')[1].split('y')[1]
    #     grad_d = prof.getElementsByTagName('benchmark-dl:University')[1].attributes['rdf:about'].value
    #     grad = grad_d.split('.')[1].split('y')[1]
    #     doc_d = prof.getElementsByTagName('benchmark-dl:University')[2].attributes['rdf:about'].value
    #     doc = doc_d.split('.')[1].split('y')[1]
    #     # personal info
    #     email = prof.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
    #     phone = prof.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
    #     res_int = prof.getElementsByTagName('benchmark-dl:researchInterest')[0].childNodes[0].nodeValue
    #     res = res_int.split('h')[1]
    #     print >> f,"INSERT INTO teachers VALUES (%s, %s, %s, %s, '%s', %s, %s, %s, '%s', '%s', %s);" % (depid, uniid, 2, tid, name, under, grad, doc, email, phone, res)
    #     # courses
    #     course_list = prof.getElementsByTagName('benchmark-dl:teacherOf')
    #     for course in course_list :
    #         c = course.attributes['rdf:resource'].value
    #         cname = c.split('/')[3]
    #         if cname[0] == 'C':
    #             ctype = 0
    #             cid = cname.split('e')[1]
    #         else:
    #             ctype = 1
    #             cid = cname.split('e')[2]
    #         print >> f,"INSERT INTO courses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, ctype, cid, tid, 2)
    #
    # # assist professors
    # for prof in assist_prof_list :
    #     nameuri = prof.attributes['rdf:about'].value
    #     name = prof.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    #     tid = name.split('r')[2]
    #     # degrees
    #     under_d = prof.getElementsByTagName('benchmark-dl:University')[0].attributes['rdf:about'].value
    #     under = under_d.split('.')[1].split('y')[1]
    #     grad_d = prof.getElementsByTagName('benchmark-dl:University')[1].attributes['rdf:about'].value
    #     grad = grad_d.split('.')[1].split('y')[1]
    #     doc_d = prof.getElementsByTagName('benchmark-dl:University')[2].attributes['rdf:about'].value
    #     doc = doc_d.split('.')[1].split('y')[1]
    #     # personal info
    #     email = prof.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
    #     phone = prof.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
    #     res_int = prof.getElementsByTagName('benchmark-dl:researchInterest')[0].childNodes[0].nodeValue
    #     res = res_int.split('h')[1]
    #     print >> f,"INSERT INTO teachers VALUES (%s, %s, %s, %s, '%s', %s, %s, %s, '%s', '%s', %s);" % (depid, uniid, 1, tid, name, under, grad, doc, email, phone, res)
    #     # courses
    #     course_list = prof.getElementsByTagName('benchmark-dl:teacherOf')
    #     for course in course_list :
    #         c = course.attributes['rdf:resource'].value
    #         cname = c.split('/')[3]
    #         if cname[0] == 'C':
    #             ctype = 0
    #             cid = cname.split('e')[1]
    #         else:
    #             ctype = 1
    #             cid = cname.split('e')[2]
    #         print >> f,"INSERT INTO courses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, ctype, cid, tid, 1)
    #
    # # lecturers
    # for prof in lect_list :
    #     nameuri = prof.attributes['rdf:about'].value
    #     name = prof.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    #     tid = name.split('r')[2]
    #     # degrees
    #     under_d = prof.getElementsByTagName('benchmark-dl:University')[0].attributes['rdf:about'].value
    #     under = under_d.split('.')[1].split('y')[1]
    #     grad_d = prof.getElementsByTagName('benchmark-dl:University')[1].attributes['rdf:about'].value
    #     grad = grad_d.split('.')[1].split('y')[1]
    #     doc_d = prof.getElementsByTagName('benchmark-dl:University')[2].attributes['rdf:about'].value
    #     doc = doc_d.split('.')[1].split('y')[1]
    #     # personal info
    #     email = prof.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
    #     phone = prof.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
    #     print >> f,"INSERT INTO teachers VALUES (%s, %s, %s, %s, '%s', %s, %s, %s, '%s', '%s', %s);" % (depid, uniid, 0, tid, name, under, grad, doc, email, phone, "NULL")
    #     # courses
    #     course_list = prof.getElementsByTagName('benchmark-dl:teacherOf')
    #     for course in course_list :
    #         c = course.attributes['rdf:resource'].value
    #         cname = c.split('/')[3]
    #         if cname[0] == 'C':
    #             ctype = 0
    #             cid = cname.split('e')[1]
    #         else:
    #             ctype = 1
    #             cid = cname.split('e')[2]
    #         print >> f,"INSERT INTO courses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, ctype, cid, tid, 0)
    #
    # # under grad students
    # for stud in under_stud_list :
    #     nameuri = stud.attributes['rdf:about'].value
    #     name = stud.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    #     studid = name.split('t')[3]
    #     # personal info
    #     email = stud.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
    #     phone = stud.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
    #     advisor = ""
    #     try:
    #         advisor = stud.getElementsByTagName('benchmark-dl:advisor')[0].attributes['rdf:resource'].value
    #         adv = advisor.split('/')[3]
    #         aid = adv.split('r')[2];
    #         atype = adv.rstrip('0123456789')
    #         if atype == "Lecturer":
    #             atypenum = 0
    #         elif atype == "AssistantProfessor":
    #             atypenum = 1
    #         elif atype == "AssociateProfessor":
    #             atypenum = 2
    #         else:
    #             atypenum = 3
    #
    #         print >> f,"INSERT INTO students VALUES (%s, %s, %s, %s, '%s', '%s', '%s', '%s', %s, %s);" % (depid, uniid, 0, studid, name, "NULL", email, phone, atypenum, aid)
    #     except:
    #         print >> f,"INSERT INTO students VALUES (%s, %s, %s, %s, '%s', '%s', '%s', '%s', '%s', '%s');" % (depid, uniid, 0, studid, name, "NULL", email, phone, "NULL", "NULL")
    #         pass
    #
    #     # courses
    #     course_list = stud.getElementsByTagName('benchmark-dl:takesCourse')
    #     for course in course_list :
    #         c = course.attributes['rdf:resource'].value
    #         cname = c.split('/')[3]
    #         if cname[0] == 'C':
    #             ctype = 0
    #             cid = cname.split('e')[1]
    #         else:
    #             ctype = 1
    #             cid = cname.split('e')[2]
    #         print >> f,"INSERT INTO takescourses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, 0, studid, ctype, cid)
    #
    # # grad students
    # for stud in grad_stud_list :
    #     nameuri = stud.attributes['rdf:about'].value
    #     name = stud.getElementsByTagName('benchmark-dl:name')[0].childNodes[0].nodeValue
    #     studid = name.split('t')[3]
    #     under_d = stud.getElementsByTagName('benchmark-dl:University')[0].attributes['rdf:about'].value
    #     under = under_d.split('.')[1].split('y')[1]
    #     # personal info
    #     email = stud.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
    #     phone = stud.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
    #     advisor = ""
    #     try:
    #         advisor = stud.getElementsByTagName('benchmark-dl:advisor')[0].attributes['rdf:resource'].value
    #         adv = advisor.split('/')[3]
    #         aid = adv.split('r')[2];
    #         atype = adv.rstrip('0123456789')
    #         if atype == "Lecturer":
    #             atypenum = 0
    #         elif atype == "AssistantProfessor":
    #             atypenum = 1
    #         elif atype == "AssociateProfessor":
    #             atypenum = 2
    #         else:
    #             atypenum = 3
    #         print >> f,"INSERT INTO students VALUES (%s, %s, %s, %s, '%s', %s, '%s', '%s', %s, %s);" % (depid, uniid, 1, studid, name, under, email, phone, atypenum, aid)
    #     except:
    #         print >> f,"INSERT INTO students VALUES (%s, %s, %s, %s, '%s', %s, '%s', '%s', '%s', '%s');" % (depid, uniid, 1, studid, name, under, email, phone, "NULL", "NULL")
    #         pass
    #
    #     # courses
    #     course_list = stud.getElementsByTagName('benchmark-dl:takesCourse')
    #     for course in course_list :
    #         c = course.attributes['rdf:resource'].value
    #         cname = c.split('/')[3]
    #         if cname[0] == 'C':
    #             ctype = 0
    #             cid = cname.split('e')[1]
    #         else:
    #             ctype = 1
    #             cid = cname.split('e')[2]
    #         print >> f,"INSERT INTO takescourses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, 1, studid, ctype, cid)
    #
    #
    # # research groups
    # for r in research_list :
    #     rg = r.attributes['rdf:about'].value
    #     rgid = rg.split('p')[3]
    #     print >> f,"INSERT INTO researchgroups VALUES (%s, %s, %s);" % (depid, uniid, rgid)
    #
    # # publications
    # for p in pub_list :
    #     name = p.attributes['rdf:about'].value
    #     a = name.split('/')[3]
    #     pub = name.split('/')[4]
    #     aid = a.split('r')[2]
    #     atype = a.rstrip('0123456789')
    #     pubid = pub.split('n')[1]
    #     if atype == "Lecturer":
    #         atypenum = 0
    #     elif atype == "AssistantProfessor":
    #         atypenum = 1
    #     elif atype == "AssociateProfessor":
    #         atypenum = 2
    #     else:
    #         atypenum = 3
    #     #author_list = p.getElementsByTagName('benchmark-dl:publicationAuthor')
    #     #a = author_list[0].attributes['rdf:resource'].value
    #     print >> f,"INSERT INTO publications VALUES (%s, %s, %s, %s, %s);" % (depid, uniid, pubid, atypenum, aid)
    #
    # # TA and RA
    # for ta in ta_list :
    #     name = ta.attributes['rdf:about'].value
    #     studid = name.split('t')[8]
    #     course = ta.getElementsByTagName('benchmark-dl:teachingAssistantOf')[0].attributes['rdf:resource'].value
    #     cid = course.split('/')[3].split('e')[1]
    #     print >> f,"INSERT INTO ta VALUES (%s, %s, %s, %s, %s);" % (depid, uniid, studid, 0, cid)
    #
    # for ra in ra_list :
    #     name = ra.attributes['rdf:about'].value
    #     studid = name.split('t')[8]
    #     print >> f,"INSERT INTO ra VALUES (%s, %s, %s);" % (depid, uniid, studid)

    f.close()
    return  # parse in parallel


def insert_persons(file, xmldoc):

    # TODO: extend
    for cls in [PROF_CLASS]:

        persons = xmldoc.getElementsByTagName(cls)
        for person in persons:
            person_uri = person.attributes['rdf:about'].value
            first_name = person.getElementsByTagName('benchmark-dl:firstName')[0].nodeValue
            last_name = person.getElementsByTagName('benchmark-dl:firstName')[0].nodeValue
            tid = first_name.split('r')[2]
            # degrees
            under_d = person.getElementsByTagName('benchmark-dl:University')[0].attributes['rdf:about'].value
            under = under_d.split('.')[1].split('y')[1]
            grad_d = person.getElementsByTagName('benchmark-dl:University')[1].attributes['rdf:about'].value
            grad = grad_d.split('.')[1].split('y')[1]
            doc_d = person.getElementsByTagName('benchmark-dl:University')[2].attributes['rdf:about'].value
            doc = doc_d.split('.')[1].split('y')[1]
            # personal info
            email = person.getElementsByTagName('benchmark-dl:emailAddress')[0].childNodes[0].nodeValue
            phone = person.getElementsByTagName('benchmark-dl:telephone')[0].childNodes[0].nodeValue
            res_int = person.getElementsByTagName('benchmark-dl:researchInterest')[0].childNodes[0].nodeValue
            res = res_int.split('h')[1]
            print >> f, "INSERT INTO Professors VALUES (%s, %s, %s, %s, '%s', %s, %s, %s, '%s', '%s', %s);" % (
                depid, uniid, 3, tid, first_name, under, grad, doc, email, phone, res)
            # courses
            course_list = person.getElementsByTagName('benchmark-dl:teacherOf')
            for course in course_list:
                c = course.attributes['rdf:resource'].value
                cname = c.split('/')[3]
                if cname[0] == 'C':
                    ctype = 0
                    cid = cname.split('e')[1]
                else:
                    ctype = 1
                    cid = cname.split('e')[2]
                print >> f, "INSERT INTO courses VALUES (%s, %s, %s, %s, %s, %s);" % (depid, uniid, ctype, cid, tid, 3)


#ppservers = ()
#job_server = pp.Server(ppservers=ppservers)

if __name__ == '__main__':
    #po = Pool()
    start_time = time.time()
    jobs = []
    for infile in glob.glob(os.path.join('.', '*.owl')):
        #print "Current file is: " + infile
        p = multiprocessing.Process(target=parsefile, args=(infile,))
        jobs.append(p)
        p.start()
    #po.apply_async(parsefile, (infile,))
    #po.close()
    #po.join()
    p.join()
    print "Time elapsed: ", time.time() - start_time, "s"
#job_server.print_stats()
	 
	
