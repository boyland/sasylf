export type ast = {
    Theorems: theoremNode[];
    Module: moduleNode;
    Syntax: syntaxesNode;
    Judgments: judgmentNode[];
};

export type theoremNode = {
    Name: string;
    Column: number;
    Line: number;
    Kind: string;
    Foralls: string[];
    Conclusion: string;
};

export type moduleNode = {
    Name: string;
    "Begin Column": number;
    "End Column": number;
    "Begin Line": number;
    "End Line": number;
};

export type syntaxesNode = {
    "Syntax Declarations": syntaxDeclarationNode[];
    Sugars: syntaxSugarNode[];
};

export type syntaxDeclarationNode = {
    Name: string;
    Column: number;
    Line: number;
    Clauses: clauseNode[];
};

export type clauseNode = {
    Name: string;
    Column: number;
    Line: number;
};

export type syntaxSugarNode = {
    Name: string;
    Column: number;
    Line: number;
};

export type judgmentNode = {
    Name: string;
    Column: number;
    Line: number;
    Form: string;
    Rules: ruleNode[];
};

export type ruleNode = {
    Premises: string[];
    Name: string;
    Conclusion: string;
    "In File": boolean;
    Column: number;
    Line: number;
};
