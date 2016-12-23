#!/usr/bin/python
import MySQLdb

# connect
db = MySQLdb.connect(host="rudisha.inf.ed.ac.uk", user="root", passwd="ammuma1234", db="gq_german")

cursor = db.cursor()

# execute SQL select statement
cursor.execute("SELECT * FROM sentences")

# commit your changes
db.commit()

# get the number of rows in the resultset
numrows = int(cursor.rowcount)

# get and display one row at a time.
for x in range(0,numrows):
    row = cursor.fetchone()
    print row[0], "-->", row[1]
