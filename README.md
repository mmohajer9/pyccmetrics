# Python Code Complexity Metrics

Python package for calculating code complexity metrics of the target source code.

## Description

With the help of this package, you can calculate the following code complexity metrics for your given source code as an input to it:

| Level   |      | Metric                                 | Variants         |
| ------- | ---- | -------------------------------------- | --------------- |
| methods | FOUT | Number of method calls (fan out)       | avg, max, total |
|         | MLOC | Method lines of code                   | avg, max, total |
|         | NBD  | Nested block depth                     | avg, max, total |
|         | PAR  | Number of parameters                   | avg, max, total |
|         | VG   | McCabe cyclomatic complexity           | avg, max, total |
|         |      |                                        |                 |
| classes | NOF  | Number of fields                       | avg, max, total |
|         | NOM  | Number of methods                      | avg, max, total |
|         | NSF  | Number of static fields                | avg, max, total |
|         | NSM  | Number of static methods               | avg, max, total |
|         |      |                                        |                 |
| files   | ACD  | Number of anonymous class declarations | value           |
|         | NOI  | Number of interfaces                   | value           |
|         | NOT  | Number of classes                      | value           |
|         | TLOC | Total lines of code                    | value           |

## Getting Started

### Support

Currently, the following source code languages is supported:

1. [x] Java
2. [ ] Python
3. [ ] JavaScript

### Dependencies

1. Python 3
2. Pip Package Manager

### Installing

```bash
pip install pyccmetrics
```

### Examples

- TBC

## Authors

**[Mohammad Mahdi Mohajer](https://github.com/mmohajer9)**

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details

## Acknowledgments & References

Inspired by the work of **[Thomas Zimmermann](https://ieeexplore.ieee.org/author/38563076700)**:

T. Zimmermann, R. Premraj and A. Zeller, "Predicting Defects for Eclipse," Third International Workshop on Predictor Models in Software Engineering (PROMISE'07: ICSE Workshops 2007), 2007, pp. 9-9, doi: 10.1109/PROMISE.2007.10. [Click for more info](https://ieeexplore.ieee.org/document/4273265).
