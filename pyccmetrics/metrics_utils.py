import re

FOUT_avg = "FOUT_avg"
FOUT_max = "FOUT_max"
FOUT_sum = "FOUT_sum"
MLOC_avg = "MLOC_avg"
MLOC_max = "MLOC_max"
MLOC_sum = "MLOC_sum"
NBD_avg = "NBD_avg"
NBD_max = "NBD_max"
NBD_sum = "NBD_sum"
PAR_avg = "PAR_avg"
PAR_max = "PAR_max"
PAR_sum = "PAR_sum"
NOF_avg = "NOF_avg"
VG_avg = "VG_avg"
VG_max = "VG_max"
VG_sum = "VG_sum"
NOF_max = "NOF_max"
NOF_sum = "NOF_sum"
NOM_avg = "NOM_avg"
NOM_max = "NOM_max"
NOM_sum = "NOM_sum"
NSF_avg = "NSF_avg"
NSF_max = "NSF_max"
NSF_sum = "NSF_sum"
NSM_avg = "NSM_avg"
NSM_sum = "NSM_sum"
NSM_max = "NSM_max"
TLOC = "TLOC"
NOI = "NOI"
ACD = "ACD"
NOT = "NOT"

initial_metrics_dict = {
    # ^ file-level metrics
    TLOC: 0,
    NOI: 0,
    ACD: 0,
    NOT: 0,
    # ^ class-level metrics
    NOF_sum: 0,
    NOF_avg: 0,
    NOF_max: 0,
    NOM_sum: 0,
    NOM_avg: 0,
    NOM_max: 0,
    NSF_sum: 0,
    NSF_avg: 0,
    NSF_max: 0,
    NSM_sum: 0,
    NSM_avg: 0,
    NSM_max: 0,
    # ^ method-level metrics
    FOUT_sum: 0,
    FOUT_avg: 0,
    FOUT_max: 0,
    MLOC_sum: 0,
    MLOC_avg: 0,
    MLOC_max: 0,
    NBD_sum: 0,
    NBD_avg: 0,
    NBD_max: 0,
    PAR_sum: 0,
    PAR_avg: 0,
    PAR_max: 0,
    VG_sum: 0,
    VG_avg: 0,
    VG_max: 0,
}

query_templates = {
    # ^ method-level metrics
    # ? number of method calls
    FOUT_avg: "",
    FOUT_max: """
    (method_invocation) @method_call
    """,
    FOUT_sum: """
    (method_invocation) @method_call
    """,
    # ? Method lines of code
    MLOC_avg: "",
    MLOC_max: """
    (method_declaration) @md
    """,
    MLOC_sum: """
    (method_declaration) @md
    """,
    # ? Nested block depth
    NBD_avg: "",
    NBD_max: "(method_declaration) @md",
    NBD_sum: "(block) @block",
    # ? Number of parameters
    PAR_avg: "",
    PAR_max: """
    (method_declaration 
        parameters: (formal_parameters) @params
    ) 
    """,
    PAR_sum: """
    (method_declaration 
        parameters: (formal_parameters) @params
    ) 
    """,
    # ? McCabe cyclomatic complexity metrics
    VG_avg: "",
    VG_max: """
        (if_statement) @if_stmt
        (for_statement) @for_stmt
        (do_statement) @do_stmt
        (while_statement) @while_stmt
        (binary_expression
            operator: "||"
        ) @bin_or
        (binary_expression
            operator: "&&"
        ) @bin_and
        (switch_block_statement_group) @case
        (ternary_expression) @tern_exp
        (catch_clause) @catch
    """,
    VG_sum: """
        (if_statement) @if_stmt
        (for_statement) @for_stmt
        (do_statement) @do_stmt
        (while_statement) @while_stmt
        (binary_expression
            operator: "||"
        ) @bin_or
        (binary_expression
            operator: "&&"
        ) @bin_and
        (switch_block_statement_group) @case
        (ternary_expression) @tern_exp
        (catch_clause) @catch
    """,
    # ^ class-level metrics
    # ? number of fields
    NOF_avg: "",
    NOF_max: """
    (class_declaration
         body: 
            (class_body 
                (field_declaration) @fd
            )
    ) 
    """,
    NOF_sum: """
    (class_declaration
         body: 
            (class_body 
                (field_declaration) @fd
            )
    ) 
    """,
    # ? number of methods
    NOM_avg: "",
    NOM_max: """
    (class_declaration
         body: 
            (class_body 
                (method_declaration) @md
            )
    ) 
    """,
    NOM_sum: """
    (class_declaration
         body: 
            (class_body 
                (method_declaration
                    name: (identifier) @md_name
                ) 
            )
    )
    """,
    # ? number of static fields
    NSF_avg: "",
    NSF_max: """
    (class_declaration
         body: 
            (class_body 
                (field_declaration
                    (modifiers) @mod
                ) 
            )
    )
    """,
    NSF_sum: """
    (class_declaration
         body: 
            (class_body 
                (field_declaration
                    (modifiers) @mod
                ) 
            )
    )
    """,
    # ? number of static methods
    NSM_avg: "",
    NSM_max: """
    (class_declaration
         body: 
            (class_body 
                (method_declaration
                    (modifiers) @mod
                ) 
            )
    )
    """,
    NSM_sum: """
    (class_declaration
         body: 
            (class_body 
                (method_declaration
                    (modifiers) @mod
                ) 
            )
    )
    """,
    # ^ file-level metrics
    # ? Total lines of code
    TLOC: "(program) @code",
    # ? Number of Interfaces
    NOI: """
        (interface_declaration 
            name: (identifier)
        ) @interface
    """,
    # ? Anonymous type declaration
    ACD: """
        (object_creation_expression 
            (class_body)
        ) @acd
    """,
    # ? Number of classes
    NOT: """
        (class_declaration 
            name: (identifier)
        ) @class_declaration
    """,
}


def remove_comments_and_blanks(string):
    # remove all occurrences streamed comments (/*COMMENT */) from string
    string = re.sub(re.compile("/\*.*?\*/", re.DOTALL), "", string)
    # remove all occurrence single-line comments (//COMMENT\n ) from string
    string = re.sub(re.compile("//.*?\n"), "", string)
    # remove blank lines
    string = re.sub(r"\n\s*\n", "\n", string)

    return string



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