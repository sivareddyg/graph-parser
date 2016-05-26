import os,sys,json,gzip

N = {}
for line in gzip.open(sys.argv[1],'r'):
  line = json.loads(line)  
  N[line['sentence']] = len(line['entities']) + 1

p = [0.0,0.0,0.0]
c = [0.0,0.0,0.0]
n = [0.0,0.0,0.0]
for line in open(sys.argv[2],'r'):
  line = line.strip().split("\t")
  sentence = line[0]
  correct = set(json.loads(line[1]))
  predicted = set(json.loads(line[2]))
  D = predicted.intersection(correct)

  ind = N[sentence]-2
  c[ind] += 1 if len(D) > 0 else 0
  p[ind] += 1 if len(predicted) > 0  else 0
  n[ind] += 1

for ind in [0,1,2]:
  prec = 1.0*c[ind]/p[ind]
  rec = 1.0*c[ind]/n[ind]
  f = 2*prec*rec/(prec+rec)
  print (ind+2),"\t",100*prec,100*rec,100*f
print "-------"
prec = 1.0*sum(c)/sum(p) #(p[0]*n[0] + p[1]*n[1] + p[2]*n[2]) / sum(n)
rec = 1.0*sum(c)/sum(n)
f = 2*prec*rec/(prec+rec)
print "all\t",100.0*prec,100.0*rec,100.0*f
