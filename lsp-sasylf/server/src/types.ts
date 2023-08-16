export type parsedData = {
    quickfixes: quickfixNode[],
    ast: ast
}

export type quickfixNode = {
    severity: string,
    error_type?: string,
    error_info?: string,
    error_message: string,
    begin_line: number,
    begin_column: number,
    end_line: number,
    end_column: number
}

export type ast = {
    name: string;
    theorems: theoremNode[];
    modules: moduleNode[];
    syntax: syntaxesNode;
    judgments: judgmentNode[];
};

export type theoremNode = {
    name: string;
    column: number;
    line: number;
    kind: string;
    foralls: string[];
    conclusion: string;
};

export type moduleNode = {
    name: string;
    begin_column: number;
    end_column: number;
    begin_line: number;
    end_line: number;
    file: string;
    ast: ast;
};

export type syntaxesNode = {
    syntax_declarations: syntaxDeclarationNode[];
    sugars: syntaxSugarNode[];
};

export type syntaxDeclarationNode = {
    name: string;
    column: number;
    line: number;
    clauses: clauseNode[];
};

export type clauseNode = {
    name: string;
    column: number;
    line: number;
};

export type syntaxSugarNode = {
    name: string;
    column: number;
    line: number;
};

export type judgmentNode = {
    name: string;
    column: number;
    line: number;
    form: string;
    rules: ruleNode[];
};

export type ruleNode = {
    premises: string[];
    name: string;
    conclusion: string;
    in_file: boolean;
    column: number;
    line: number;
    file: string;
};
