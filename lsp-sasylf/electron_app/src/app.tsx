import React, { useState, useEffect, useContext } from "react";
import { createRoot } from "react-dom/client";
import { ast } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import { DroppedContext } from "./components/state";
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
			.then((compUnit: ast | null) => compUnitHandler(compUnit));
	}, []);

	const compUnitHandler = (newCompUnit: ast | null) => setCompUnit(newCompUnit);

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
			<DroppedContext.Provider value={[dropped, removeHandler]}>
				<ProofArea />
			</DroppedContext.Provider>
		</DndContext>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
