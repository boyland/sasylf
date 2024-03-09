import { line } from "../types";

export const deleteElement = <T>(arr: T[], index: number) => [
	...arr.slice(0, index),
	...arr.slice(index + 1),
];

export function extractPremise(conclusion: string, tree: line) {
	for (const premise of tree.premises)
		if (premise.conclusion === conclusion) return premise;

	return null;
}

export function getChildrenIds(proofNode: Element | null) {
	const res: number[] = [];

	if (!proofNode) return res;

	const premiseNodes = proofNode.getElementsByClassName("premises");

	for (const premise of premiseNodes) {
		const containers = premise.getElementsByClassName("premise");
		const container = containers[containers.length - 1];
		res.push(parseInt(container.id));
	}

	return res;
}

export function getParentId(proofNode: Element | null) {
	if (!proofNode || proofNode.classList.contains("root-node")) return -1;

	const res = proofNode.parentElement?.parentElement?.parentElement?.id;

	return res ? parseInt(res) : -1;
}

export function getTree(proofNode: Element | null): line | null {
	if (!proofNode) return null;

	const containers = proofNode.getElementsByClassName("conclusion");
	const container = containers[containers.length - 1];
	const conclusion = container.getElementsByTagName("span")[0].textContent;
	const name = container.getElementsByTagName("input")[0].value;
	const rules = proofNode.getElementsByClassName("rule");
	const rule = rules[rules.length - 1].textContent;

	let premises: line[] = [];
	const premiseNodes = proofNode.getElementsByClassName("premises");

	for (const premise of premiseNodes) {
		const prev = getTree(premise);

		if (prev) premises.push(prev);
	}

	if (!conclusion || !rule) return null;

	return { conclusion, name, rule, premises };
}

type lineText = {
	conclusion: string;
	name: string;
	rule: string;
	premises: line[];
};

function theoremHelper(root: Element | null): lineText[] {
	if (!root) return [];

	const tree = getTree(root);

	if (!tree) return [];

	function bfs(l: line) {
		let res: line[] = [l];

		for (const premise of l.premises) res = bfs(premise).concat([l]);

		return res;
	}

	return bfs(tree);
}

export function createTheorem(
	theoremName: string,
	quantifiers: string,
	rootNode: Element | null,
) {
	const nodes = theoremHelper(rootNode);

	if (!nodes) return;

	const conclusion = nodes[nodes.length - 1].conclusion;
	let content = `theorem ${theoremName.trim()} : ${quantifiers.trim()} exists ${conclusion.trim()}.\n`;
	for (const node of nodes) {
		content += `${node.name.trim()}: ${node.conclusion.trim()} by rule ${node.rule.trim()}${
			node.premises.length === 0
				? ""
				: ` on ${node.premises.map((premise) => premise.name).join(", ")}`
		}\n`;
	}
	return content;
}

export function createTheorems(
	theoremNames: string[],
	quantifiers: string[],
	wanted: Set<number>,
	dom: HTMLDivElement | null,
): string {
	const rootNodes = dom?.getElementsByClassName("root-node");
	// TODO : assert len of rootNodes and theoremNames
	if (!rootNodes) return "";
	let content: string = "";
	for (let i = 0; i < rootNodes.length; ++i) {
		if (wanted.has(i))
			content += createTheorem(theoremNames[i], quantifiers[i], rootNodes[i]);
	}
	return content;
}
