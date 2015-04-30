[QueryItem="q1"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?y
where{
?x :isMemberOf ?y.
}

[QueryItem="q2"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?y
where{
?x :isHeadOf ?y.
}

[QueryItem="q3"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?y
where{
?x :hasMajor ?y.
}

[QueryItem="q4"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?y
where{
?x :hasAlumnus ?y.
}

[QueryItem="q5"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x
where{
?x a :Man.
}

[QueryItem="q6"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x 
where{
?x a :Woman.
}

[QueryItem="q7"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x
where{
?x a :Person.
}

[QueryItem="q8"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?name
where{
?x a :Woman; :firstName ?name.
}

[QueryItem="q9"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?y
where{
?x a :Man; :isHeadOf ?y.
}

[QueryItem="q10"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
select ?x ?y
where{
?x :like ?y.
}

[QueryItem="q11"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
SELECT ?x ?y ?name
WHERE{
?x :isFriendOf ?y; :firstName ?name.
}

[QueryItem="q12"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
SELECT ?x ?y
WHERE{
?x a :Student; :isFriendOf ?y.
}

[QueryItem="q13"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
SELECT ?x ?y ?name
WHERE{
?x :isCrazyAbout ?y. ?x :firstName ?name.
}

[QueryItem="q14"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
SELECT ?x 
WHERE{
?x a :Article.
}

[QueryItem="q15"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
SELECT ?x ?y
WHERE{
?x a :Article; :publicationAuthor ?y.
}

[QueryItem="q16"]
PREFIX : <http://uob.iodt.ibm.com/univ-bench-dl.owl#>
SELECT ?x ?y ?z ?q
WHERE{
?x:hasDegreeFrom ?y; :isFriendOf ?z.
?z :like ?q
}
