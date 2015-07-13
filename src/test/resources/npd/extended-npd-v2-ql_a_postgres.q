[QueryItem="Member"]
PREFIX : <http://sws.ifi.uio.no/vocab/npd-v2#>
SELECT * WHERE {
?x a :Member
}

[QueryItem="Group"]
PREFIX : <http://sws.ifi.uio.no/vocab/npd-v2#>
SELECT * WHERE {
?x a :Group
} LIMIT 10

[QueryItem="Formation"]
PREFIX : <http://sws.ifi.uio.no/vocab/npd-v2#>
SELECT * WHERE {
?x a :Formation
} LIMIT 10
