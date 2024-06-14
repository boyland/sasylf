import React, { useState } from "react";
import Button from "react-bootstrap/Button";
import CloseButton from "react-bootstrap/CloseButton";
import Collapse from "react-bootstrap/Collapse";
import { FaArrowRightLong } from "react-icons/fa6";
import { ast, judgmentNode, ruleNode, theoremNode, moduleNode } from "../types";
import Draggable from "./draggable";

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

function BankCollapse({
	children,
	name,
	variant,
	title,
}: {
	children: React.ReactNode;
	name: string;
	variant: string;
	title: string;
}) {
	const [open, setOpen] = useState(false);

	const onChange = () => {
		const event = new Event("resize");
		document.dispatchEvent(event);
	};

	return (
		<>
			<Button
				className="m-1 rule-like"
				onClick={() => setOpen(!open)}
				aria-expanded={open}
				aria-controls={name}
				variant={variant}
			>
				<code className="rule-like-text">{title}</code>
			</Button>
			<Collapse in={open} onEntering={onChange} onExited={onChange}>
				<div id={name}>{children}</div>
			</Collapse>
		</>
	);
}

function Judgment(props: { judgment: judgmentNode }) {
	const rules = props.judgment.rules.map((rule) => ruleToText(rule));
	const rulesElements = rules.map((rule, ind) => (
		<Draggable key={ind} id={rule[1]} data={{ type: "rule", text: rule[1] }}>
			<RuleLike text={rule[0]} />
		</Draggable>
	));

	return (
		<BankCollapse
			name={props.judgment.name}
			title={`judgment ${props.judgment.name}: ${props.judgment.form}`}
			variant="outline-secondary"
		>
			<div id={props.judgment.name}>
				<div className="d-flex flex-column">{rulesElements}</div>
			</div>
		</BankCollapse>
	);
}

function Module(props: { module: moduleNode }) {
	return (
		<BankCollapse
			title={`module ${props.module.name}`}
			variant="outline-success"
			name={props.module.name}
		>
			<RuleLikes compUnit={props.module.ast} />
		</BankCollapse>
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

function RuleLikes(props: {
	compUnit: ast;
	bankRef?: React.RefObject<HTMLDivElement>;
}) {
	const theorems = props.compUnit.theorems.map((value) => theoremToText(value));

	const theoremsElements = theorems.map((thm, ind) => (
		<Draggable key={ind} id={thm[1]} data={{ type: "rule", text: thm[1] }}>
			<RuleLike text={thm[0]} />
		</Draggable>
	));

	const judgments = props.compUnit.judgments.map((value, ind) => (
		<Judgment key={ind} judgment={value} />
	));

	const modules = props.compUnit.modules.map((value, ind) => (
		<Module key={ind} module={value} />
	));

	return (
		<div className="rule-likes" ref={props.bankRef}>
			{theoremsElements}
			{judgments}
			{modules}
		</div>
	);
}

interface BankProps {
	compUnit: ast | undefined;
	toggleShow: () => void;
	bankRef: React.RefObject<HTMLDivElement>;
	width: number;
	setWidth: React.Dispatch<React.SetStateAction<number>>;
}

export default function Bank(props: BankProps) {
	const [isResizing, setIsResizing] = useState(false);

	const handleClose = () => props.setWidth(0);
	const handleShow = () => props.setWidth(props.bankRef.current!.scrollWidth);

	const startResizing = (_: React.MouseEvent) => {
		setIsResizing(true);
	};

	const stopResizing = () => {
		setIsResizing(false);
	};

	const resize = (e: MouseEvent) => {
		if (isResizing)
			props.setWidth(
				e.clientX - props.bankRef.current!.getBoundingClientRect().left,
			);
	};

	React.useEffect(() => {
		window.addEventListener("mousemove", resize);
		window.addEventListener("mouseup", stopResizing);

		return () => {
			window.removeEventListener("mousemove", resize);
			window.removeEventListener("mouseup", stopResizing);
		};
	}, [resize, stopResizing]);

	return props.compUnit ? (
		<>
			<Button variant="outline-dark" className="open-bank" onClick={handleShow}>
				<FaArrowRightLong size={25} />
			</Button>
			<div
				className="bank-outer"
				style={{ width: props.width }}
			>
				<div className="bank-header">
					<h3 className="no-wrap">Rules Bank</h3>
					<CloseButton onClick={handleClose} />
				</div>
				<RuleLikes compUnit={props.compUnit} bankRef={props.bankRef} />
			</div>
			<div className="bank-resizer" onMouseDown={startResizing} />
		</>
	) : null;
}
