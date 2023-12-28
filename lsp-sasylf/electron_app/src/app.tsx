import React, { useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { ast, tab } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import { DndContext, DragStartEvent, UniqueIdentifier } from "@dnd-kit/core";
import { Tab, Tabs, TabList, TabPanel } from "react-tabs";

export default function MyApp() {
	const [tabs, setTabs] = useState<Array<tab>>([]);
	const [activeId, setActiveId] = useState<UniqueIdentifier | null>(null);

	const addTab = (compUnit: ast | null, filePath: string) => {
		let maxId: number = -1;
		let occurences: number = 0;
		for (const e of tabs) {
			maxId = Math.max(e.id, maxId);
			if (e.filePath === filePath) occurences += 1;
		}
		const fileName = filePath.replace(/^.*[\\/]/, "");
		setTabs(
			tabs.concat([
				{
					ast: compUnit,
					id: maxId + 1,
					filePath,
					name: fileName + (occurences === 0 ? "" : ` (${occurences})`),
				},
			]),
		);
	};

	(window as any).electronAPI.addAST(({ compUnit, filePath }) =>
		addTab(compUnit, filePath),
	);

	function handleDragStart(event: DragStartEvent) {
		setActiveId(event.active.id);
	}

	function handleDragEnd() {
		setActiveId(null);
	}

	function handleClose(id: number) {
		setTabs(tabs.filter((element) => element.id !== id));
	}

	return (
		<Tabs>
			<TabList>
				{tabs.map((element) => (
					<Tab>{element.name}</Tab>
				))}
			</TabList>

			{tabs.map((element) => (
				<TabPanel>
					<DndContext onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
						<Bank compUnit={element.ast} activeId={activeId} />
						<ProofArea />
					</DndContext>
					<button
						className="btn btn-primary"
						onClick={() => handleClose(element.id)}
						style={{ textAlign: "right", width: "fit-content" }}
					>
						Close
					</button>
				</TabPanel>
			))}
		</Tabs>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
