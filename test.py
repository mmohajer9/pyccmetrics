import itertools
from tree_sitter import Language, Parser

from pprint import pprint as print

JAVA_LANG = Language("lib/my-languages.so", "java")

parser = Parser()
parser.set_language(JAVA_LANG)

file_content = ""

with open("./tests/test10.java", encoding="utf8", errors="ignore") as f:
    file_content = f.read()


tree = parser.parse(bytes(file_content, "utf8"))

root_node = tree.root_node


sexp = root_node.sexp()

query_str = """

     (block) @block
    
    
"""

query = JAVA_LANG.query(query_str)

captures = query.captures(root_node)

node = captures[0][0]

def find_depth(node):
    
    sum_of_depths = 0
    
    if node.type == "block":
        sum_of_depths += 1
        for child in node.children:
            sum_of_depths += find_depth(child)
    
    elif node.children:
        for child in node.children:
            sum_of_depths += find_depth(child)

    
    return sum_of_depths
    
    
print(captures)
print(len(captures))
# print(sexp)
# print(root_node)

# sum_of_depth = 0
# def find_depth(node):
    
#     global sum_of_depth
    
#     if node.type == "block":
#         sum_of_depth += 1
    
#     children = node.children
    
#     for child in children:
#         find_depth(child)


# find_depth(node)

# print(sum_of_depth)
        
    



# print(find_depth(node))

    




# print(captures[0][0].text)

# static_list = [i[0].parent.parent.id for i in captures if "static" in str(i[0].text)]
# counts = [len(list(g[1])) for g in itertools.groupby(static_list)]
# maximum = max(counts)

# methods_list = []

# for item in captures:
#     node = item[0]
#     parent = node.parent
#     while(parent.type != "method_declaration"):
#         parent = parent.parent
#         if parent is None:
#             break
#     if parent is not None:
#         methods_list.append(parent.id)

# counts = [len(list(g[1])) for g in itertools.groupby(methods_list)]
# maximum = max(counts)

# print(maximum)


    
    
