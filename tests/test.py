import itertools
from tree_sitter import Language, Parser

from pprint import pprint as print

JAVA_LANG = Language("lib/my-languages.so", "java")

parser = Parser()
parser.set_language(JAVA_LANG)

file_content = ""

with open("./target/java/test10.java", encoding="utf8", errors="ignore") as f:
    file_content = f.read()


tree = parser.parse(bytes(file_content, "utf8"))

root_node = tree.root_node


sexp = root_node.sexp()

query_str = """

     (block) @block
    
    
"""

query = JAVA_LANG.query(query_str)

captures = query.captures(root_node)
    
print(captures)
print(len(captures))



    
    
