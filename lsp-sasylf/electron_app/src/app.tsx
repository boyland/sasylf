import React, { useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { ast } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import Canvas from "./components/canvas";
import { DndContext, DragStartEvent, UniqueIdentifier } from "@dnd-kit/core";

export default function MyApp() {
	const [compUnit, setCompUnit] = useState<ast | null>(null);
	const [activeId, setActiveId] = useState<UniqueIdentifier | null>(null);

	const myHandler = (newCompUnit: ast | null) => {
		setCompUnit(newCompUnit);
	};

	useEffect(() => {
		(window as any).electronAPI
			.getAST()
			.then((compUnit: ast | null) => myHandler(compUnit));
	}, []);

	function handleDragStart(event: DragStartEvent) {
		setActiveId(event.active.id);
	}

	function handleDragEnd() {
		setActiveId(null);
	}

	return (
		<DndContext onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
			<Bank compUnit={compUnit} activeId={activeId} />
			<ProofArea />
			<Canvas />
		</DndContext>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
