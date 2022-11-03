import itertools
from tree_sitter import Language, Parser
from .metrics_utils import (
    find_depth,
    query_templates,
    initial_metrics_dict,
    remove_comments_and_blanks,
)
from pathlib import Path


class Metrics:
    def __init__(self, file_path):

        self.java_lang = Language(Path(__file__).parent / "lib/my-languages.so", "java")
        self.parser = Parser()

        self.parser.set_language(self.java_lang)

        self.file_content = ""
        self.file_path = file_path
        self.read_file()

        self.tree = ""
        self.parse_tree()

        # * note: order of this list is important -> it indicates the order of calculation
        self.metrics_list = [item for item in initial_metrics_dict]
        self.metrics_dict = {**initial_metrics_dict}
        self.query_templates = {**query_templates}

        # current query and its captures
        self.query = ""
        self.captures = ""

    def read_file(self):
        with open(self.file_path, encoding="utf8", errors="ignore") as f:
            self.file_content = f.read()

        return self.file_content

    def parse_tree(self):
        self.tree = self.parser.parse(bytes(self.file_content, "utf8"))
        self.root_node = self.tree.root_node
        return self.tree

    def create_query(self, query_str):
        self.query = self.java_lang.query(query_str)
        return self.query

    def perform_query(self):
        self.captures = self.query.captures(self.root_node)
        return self.captures

    def calculate(self):
        try:
            for metric in self.metrics_list:
                # getting the proper method dynamically
                method = getattr(self, f"_calc_{metric}")
                # setting the right query
                self.create_query(self.query_templates[metric])
                # performing the query and capturing the results from it
                self.perform_query()
                # executing the method and storing the result in the dict
                self.metrics_dict[metric] = method()

            return 0
        except Exception as e:
            raise e

    # ^ method-level metrics

    def _calc_FOUT_avg(self):
        try:
            return (self.metrics_dict["FOUT_sum"]) / (self.metrics_dict["NOM_sum"])
        except:
            return 0

    def _calc_FOUT_max(self):
        methods_list = []

        for item in self.captures:
            node = item[0]
            parent = node.parent
            while(parent.type != "method_declaration"):
                parent = parent.parent
                if parent is None:
                    break
            if parent is not None:
                methods_list.append(parent.id)

        counts = [len(list(g[1])) for g in itertools.groupby(methods_list)]
        maximum = max(counts,default=0)
        return maximum

    def _calc_FOUT_sum(self):
        return len(self.captures)

    def _calc_MLOC_avg(self):
        try:
            return (self.metrics_dict["MLOC_sum"] / self.metrics_dict["NOM_sum"])
        except:
            return 0

    def _calc_MLOC_max(self):
        method_sizes = [(item[0].end_point[0] - item[0].start_point[0]) for item in self.captures]
        maximum = max(method_sizes,default=0)
        return maximum


    def _calc_MLOC_sum(self):
        sum_of_lines = 0
        for item in self.captures:
            node = item[0]
            item_mloc = node.end_point[0] - node.start_point[0]
            sum_of_lines += item_mloc
        return sum_of_lines

    def _calc_NBD_avg(self):
        try:
            return (self.metrics_dict["NBD_sum"]) / (self.metrics_dict["NOM_sum"])
        except Exception as e:
            return 0

    def _calc_NBD_max(self):
        try:
            nodes = [item[0] for item in self.captures]
            depths_per_node_id = {}
            
            sum_of_all_nodes_depth = 0
            for node in nodes:
                depths_per_node_id[node.id] = find_depth(node)
                sum_of_all_nodes_depth = depths_per_node_id[node.id]
            
            return max(depths_per_node_id.values(),default=0)
        except Exception as e:
            return 0
         

    def _calc_NBD_sum(self):
        return len(self.captures)
        # nodes = [item[0] for item in self.captures]
        
        # sum_of_all_nodes_depth = 0
        # for node in nodes:
        #     sum_of_all_nodes_depth += find_depth(node)
            
        # return sum_of_all_nodes_depth

    def _calc_PAR_avg(self):
        try:
            return (self.metrics_dict["PAR_sum"]) / (self.metrics_dict["NOM_sum"])
        except Exception as e:
            return 0
        
        

    def _calc_PAR_max(self):
        max_count = 0
        for item in self.captures:
            node = item[0]
            text = node.text.decode()
            text = text[1:-1]
            params = text.split(",")
            param_len = len(params)
            if param_len > max_count:
                max_count = param_len

        return max_count

    def _calc_PAR_sum(self):
        count = 0
        for item in self.captures:
            node = item[0]
            text = node.text.decode()
            text = text[1:-1]
            params = text.split(",")
            if text:
                count += len(params)
        return count

    def _calc_VG_avg(self):
        try:
            return (self.metrics_dict["VG_sum"] / self.metrics_dict["NOM_sum"])
        except:
            return 0

    #* parent is not none should be checked
    #* check foutmax but the query is different.
    def _calc_VG_max(self):
        methods_list = []
        for item in self.captures:
            node = item[0]
            parent = node.parent
            while(parent.type != "method_declaration"):
                parent = parent.parent
                if parent is None:
                    break
            if parent is not None:
                methods_list.append(parent.id)

        counts = [len(list(g[1])) for g in itertools.groupby(methods_list)]
        maximum = max(counts,default=0)
        return maximum

    def _calc_VG_sum(self):
        return len(self.captures)

    # ^ class-level metrics

    def _calc_NOF_avg(self):
        try:
            return (self.metrics_dict["NOF_sum"]) / (self.metrics_dict["NOT"])
        except:
            return 0

    def _calc_NOF_max(self):

        NOF_per_class = {}

        for item in self.captures:
            node = item[0]
            parent = node.parent
            if parent.id in NOF_per_class:
                NOF_per_class[parent.id] += 1
            else:
                NOF_per_class[parent.id] = 1

        return max(NOF_per_class.values(),default=0)

    def _calc_NOF_sum(self):
        return len(self.captures)

    def _calc_NOM_avg(self):
        try:
            return (self.metrics_dict["NOM_sum"]) / (self.metrics_dict["NOT"])
        except:
            return 0

    def _calc_NOM_max(self):
        NOM_per_class = {}

        for item in self.captures:
            node = item[0]
            parent = node.parent
            if parent.id in NOM_per_class:
                NOM_per_class[parent.id] += 1
            else:
                NOM_per_class[parent.id] = 1

        return max(NOM_per_class.values(),default=0)

    def _calc_NOM_sum(self):
        return len(self.captures)

    def _calc_NSF_avg(self):
        try:
            return (self.metrics_dict["NSF_sum"]) / (self.metrics_dict["NOT"])
        except:
            return 0

    def _calc_NSF_max(self):
        static_list = [
            i[0].parent.parent.id for i in self.captures if "static" in str(i[0].text)
        ]
        counts = [len(list(g[1])) for g in itertools.groupby(static_list)]
        maximum = max(counts,default=0)
        return maximum

    def _calc_NSF_sum(self):
        return len([i for i in self.captures if "static" in str(i[0].text)])

    def _calc_NSM_avg(self):
        try:
            return (self.metrics_dict["NSM_sum"]) / (self.metrics_dict["NOT"])
        except:
            return 0

    def _calc_NSM_max(self):
        static_list = [
            i[0].parent.parent.id for i in self.captures if "static" in str(i[0].text)
        ]
        counts = [len(list(g[1])) for g in itertools.groupby(static_list)]
        maximum = max(counts,default=0)
        return maximum

    def _calc_NSM_sum(self):
        return len([i for i in self.captures if "static" in str(i[0].text)])

    # ^ file-level metrics

    def _calc_TLOC(self):
        self.file_content = remove_comments_and_blanks(self.file_content)
        self.parse_tree()
        start_line = self.root_node.start_point[0]
        end_line = self.root_node.end_point[0]
        return end_line - start_line

    def _calc_NOI(self):
        return len(self.captures)

    def _calc_ACD(self):
        return len(self.captures)

    def _calc_NOT(self):
        return len(self.captures)
