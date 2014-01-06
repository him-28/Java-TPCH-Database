Java-TPCH-Database
==================

Part I: Notice & License
This project is the homework solution of Rice University's Computer Science Department coursework Comp 430 : Database System. The authors are the instructor cmj4 (cmj4@rice.edu), and the student jz33 (junzhengrice@gmail.com), as well as unknown open source file contributers. Please contact jz33 for reusing on any Rice University's related coursework projects, otherwise it is free-distributed.

Part II: File Discrimination
A401.pdf: Requirement of the Part I of this project. Author: cmj4
A402.pdf: Requirement of the Part II of this project. Author: cmj4
antlr-3.2.jar: tool to transfer the grammar file (SQL.g) into lexer & parser. Author: www.antlr.org
cppDir/*: the native cpluplus files for running step. Author: cmj4
Catalog.xml: the skeleton of TPC-H benchmark table. Author: http://www.tpc.org/tpch/
queries: 15 cases for Part I testing. Author: cmj4
SQL.g: Grammar file. Author: cmj4

src/interpreter/*: The main function. Author: cmj4, jz33
src/optimizer/*: The optimizer, solution of Part II. Author: jz33
src/parser/SemanticChecker.java: The semantic checker, solution of Part I. Author: jz33
src/parser/[others]: The helper containers holding specific entities of SQL. Author: cmj4, jz33
src/runner/Selection.java, Join.java, Grouping.java: the 3 runners compiling & running cplusplus files. Author: cmj4
src/runner/Runner.java: Single test case for Part II. Author: cmj4
src/runner/[others]: The helper containers holding specific entities of SQL. Author: cmj4, jz33

tests/*: 10 cases for Part II testing. Author: cmj4
tpch-dbgen/* : The TPC-H benchmark table generator. Author: http://www.tpc.org/tpch/

Part III: Run Instruction
1. Generate lexer & parser. At root directory ("JavaDB"), execute:
    java -cp antlr-3.2.jar org.antlr.Tool SQL.g
This will generate SQLLexer.java, SQLParser.java. Move to "src/parser"
2. Disable the optimizer at line 67 of "src/interpreter/Interpreter.java"
3. Run Interpreter. Test cases in "queries". This implements Part I of this project
4. Generate TCP-H tables at "tpch-dbgen". Instructions please refer to "tpch-dbgen/README"
5. Enable optimizer at "src/interpreter/Interpreter.java"
6. Run stock test case "src/runner/Runner.java"
7. Run Interpreter. Test cases in "tests/*". This implements Part II of this project
