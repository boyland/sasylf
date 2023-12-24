import React, { useState } from "react";
import Button from "react-bootstrap/Button";
import Card from "react-bootstrap/Card";
import Collapse from "react-bootstrap/Collapse";
import Offcanvas from "react-bootstrap/Offcanvas";
import { FaArrowRightLong } from "react-icons/fa6";
import { ast, judgmentNode, ruleNode, theoremNode } from "../types";
import Draggable from "./draggable";
import { DragOverlay, UniqueIdentifier } from "@dnd-kit/core";
import { snapCenterToCursor } from "@dnd-kit/modifiers";

interface RuleLikeProps {
	text: React.JSX.Element;
	className?: string;
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
		<Draggable key={ind} id={rule[1]}>
			<RuleLike text={rule[0]} />
		</Draggable>
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

function ruleToText(rule: ruleNode): [React.JSX.Element, string] {
	let max_len = 0;
	const lines: string[] = [];

	for (const premise of rule.premises)
		max_len = Math.max(max_len, premise.length);

	max_len = Math.max(max_len, rule.conclusion.length);

	for (const premise of rule.premises) lines.push(premise);

	lines.push(`${"-".repeat(max_len)} ${rule.name}`);
	lines.push(rule.conclusion);

	return [
		<>
			{lines.map((line, ind) => (
				<div key={ind}>
					<span>{line}</span>
					<br />
				</div>
			))}
		</>,
		rule.name,
	];
}

function theoremToText(theorem: theoremNode): [React.JSX.Element, string] {
	let max_len = 0;
	const lines: string[] = [];

	for (const forall of theorem.foralls)
		max_len = Math.max(max_len, forall.length);

	max_len = Math.max(max_len, theorem.conclusion.length);

	for (const forall of theorem.foralls) lines.push(forall);

	lines.push(`${"-".repeat(max_len)} ${theorem.name}`);
	lines.push(theorem.conclusion);

	return [
		<>
			{lines.map((line, ind) => (
				<div key={ind}>
					<span>{line}</span>
					<br />
				</div>
			))}
		</>,
		theorem.name,
	];
}

interface ASTProps {
	compUnit: ast;
}

function RuleLikes(props: ASTProps) {
	const theorems = props.compUnit.theorems.map((value) => theoremToText(value));

	const theoremsElements = theorems.map((thm, ind) => (
		<Draggable key={ind} id={thm[1]}>
			<RuleLike text={thm[0]} />
		</Draggable>
	));

	const judgments = props.compUnit.judgments.map((value, ind) => (
		<Judgment key={ind} judgment={value} />
	));

	return (
		<div className="d-flex flex-column exact">
			{theoremsElements}
			{judgments}
		</div>
	);
}

interface BankProps {
	compUnit: ast | null;
	activeId: UniqueIdentifier | null;
}

export default function Bank(props: BankProps) {
	const [show, setShow] = useState(false);

	const handleClose = () => setShow(false);
	const handleShow = () => setShow(true);

	return props.compUnit ? (
		<>
			<Button variant="outline-dark" className="open-bank" onClick={handleShow}>
				<FaArrowRightLong size={25} />
			</Button>
			<Offcanvas
				show={show}
				onHide={handleClose}
				backdrop={false}
				id="bank-canvas"
			>
				<Offcanvas.Header closeButton>
					<Offcanvas.Title>Rules Bank</Offcanvas.Title>
				</Offcanvas.Header>
				<Offcanvas.Body>
					<RuleLikes compUnit={props.compUnit} />
					<DragOverlay modifiers={[snapCenterToCursor]}>
						{props.activeId ? (
							<Card body className="exact" border="dark" text="dark">
								<code className="rule-like-text">{props.activeId}</code>
							</Card>
						) : null}
					</DragOverlay>
				</Offcanvas.Body>
			</Offcanvas>
		</>
	) : null;
}
