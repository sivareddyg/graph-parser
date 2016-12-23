import sys
import json
import random
import os
import sqlite3

items = []
for line in sys.stdin:
    sent = json.loads(line)
    items.append((sent['qid'], sent['sentence']))

random.seed(1)
random.shuffle(items)
random.shuffle(items)
random.shuffle(items)
random.shuffle(items)

conn = sqlite3.connect('working/annotations.db')
c = conn.cursor()

# Create table
c.execute('''CREATE TABLE annotators
             (email text PRIMARY KEY NOT NULL, name text, salary real DEFAULT 0.0)''')
c.execute('''CREATE TABLE sentences
             (sentid INTEGER PRIMARY KEY, qid INTEGER, sentence text NOT NULL, translated integer DEFAULT 0, translation text, startstamp INTEGER DEFAULT 0, endstamp INTEGER, annotator text)''')

for item in items:
	# Insert a row of data
	value = {}
	value['qid'] = item[0]
	value['sentence'] = item[1]
	c.execute("INSERT INTO sentences (qid,sentence) VALUES (:qid,:sentence);", value)

# Save (commit) the changes
conn.commit()

# We can also close the connection if we are done with it.
# Just be sure any changes have been committed or they will be lost.
conn.close()
