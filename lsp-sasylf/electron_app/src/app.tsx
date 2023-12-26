import React, { useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { ast } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import {
	DndContext,
	DragEndEvent,
	DragStartEvent,
	UniqueIdentifier,
} from "@dnd-kit/core";
import { snapCenterToCursor } from "@dnd-kit/modifiers";

export default function MyApp() {
	const [compUnit, setCompUnit] = useState<ast | null>(null);
	const [activeId, setActiveId] = useState<UniqueIdentifier | null>(null);
	const [dropped, setDropped] = useState({});

	useEffect(() => {
		(window as any).electronAPI
			.getAST()
			.then((compUnit: ast | null) => myHandler(compUnit));
	}, []);

	const myHandler = (newCompUnit: ast | null) => setCompUnit(newCompUnit);

	const removeHandler = (id: number) => {
		const newDropped = { ...dropped };
		delete newDropped[id];
		setDropped(newDropped);
	};

	const handleDragStart = (event: DragStartEvent) =>
		setActiveId(event.active.id);

	const handleDragEnd = (event: DragEndEvent) => {
		setActiveId(null);

		if (event.over && !(event.over.id in dropped))
			setDropped({ ...dropped, [event.over.id]: event.active.id });
	};

	return (
		<DndContext
			modifiers={[snapCenterToCursor]}
			onDragStart={handleDragStart}
			onDragEnd={handleDragEnd}
		>
			<Bank compUnit={compUnit} activeId={activeId} />
			<ProofArea dropped={dropped} remove={removeHandler} />
		</DndContext>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
