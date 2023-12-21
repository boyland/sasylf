import React, { useState } from "react";
import Button from "react-bootstrap/Button";
import Collapse from "react-bootstrap/Collapse";
import { ast, judgmentNode, ruleNode, theoremNode } from "../types";

interface RuleLikeProps {
	text: React.JSX.Element;
}

function RuleLike(props: RuleLikeProps) {
	return (
		<Button variant="outline-primary" className="m-1 rule-like">
			<code className="rule-like-text">{props.text}</code>
		</Button>
	);
}

interface JudgmentProps {
	judgment: judgmentNode;
}

function Judgment(props: JudgmentProps) {
	const rules = props.judgment.rules.map((rule) => ruleToText(rule));
	const rulesElements = rules.map((rule, ind) => (
		<div key={ind}>
			<RuleLike text={rule} />
		</div>
	));
	const [open, setOpen] = useState(false);

	return (
		<>
			<Button
				className="m-1 rule-like"
				onClick={() => setOpen(!open)}
				aria-expanded={open}
				aria-controls={props.judgment.name}
				variant="outline-secondary"
			>
				<code className="rule-like-text">{`judgment ${props.judgment.name}: ${props.judgment.form}`}</code>
			</Button>
			<Collapse in={open}>
				<div id={props.judgment.name}>
					<div className="d-flex flex-column">{rulesElements}</div>
				</div>
			</Collapse>
		</>
	);
}

function ruleToText(rule: ruleNode): React.JSX.Element {
	let max_len = 0;
	const lines: string[] = [];

	for (const premise of rule.premises)
		max_len = Math.max(max_len, premise.length);

	max_len = Math.max(max_len, rule.conclusion.length);

	for (const premise of rule.premises) lines.push(premise);

	lines.push(`${"-".repeat(max_len)} ${rule.name}`);
	lines.push(rule.conclusion);

	return (
		<>
			{lines.map((line, ind) => (
				<div key={ind}>
					<span>{line}</span>
					<br />
				</div>
			))}
		</>
	);
}

function theoremToText(theorem: theoremNode): React.JSX.Element {
	let max_len = 0;
	const lines: string[] = [];

	for (const forall of theorem.foralls)
		max_len = Math.max(max_len, forall.length);

	max_len = Math.max(max_len, theorem.conclusion.length);

	for (const forall of theorem.foralls) lines.push(forall);

	lines.push(`${"-".repeat(max_len)} ${theorem.name}`);
	lines.push(theorem.conclusion);

	return (
		<>
			{lines.map((line, ind) => (
				<div key={ind}>
					<span>{line}</span>
					<br />
				</div>
			))}
		</>
	);
}

interface RuleLikesProps {
	compUnit: ast;
}

export function RuleLikes(props: RuleLikesProps) {
	const theorems = props.compUnit.theorems.map((value) => theoremToText(value));

	const theoremsElements = theorems.map((thm, ind) => (
		<div key={ind}>
			<RuleLike text={thm} key={ind} />
		</div>
	));

	const judgments = props.compUnit.judgments.map((value, ind) => (
		<div key={ind}>
			<Judgment judgment={value} />
		</div>
	));

	return (
		<div className="d-flex flex-column bank">
			{theoremsElements}
			{judgments}
		</div>
	);
}
