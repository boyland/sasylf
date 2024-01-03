import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { ast, tab } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import Canvas from "./components/canvas";
import Export from "./components/export";
import { DroppedContext } from "./components/state";
import {
	DndContext,
	DragEndEvent,
	DragStartEvent,
	UniqueIdentifier,
} from "@dnd-kit/core";
import { snapCenterToCursor } from "@dnd-kit/modifiers";
import Tab from "react-bootstrap/Tab";
import Nav from "react-bootstrap/Nav";
import CloseButton from "react-bootstrap/CloseButton";

export default function MyApp() {
	const [tabs, setTabs] = useState<tab[]>([]);
	const [activeId, setActiveId] = useState<UniqueIdentifier | null>(null);
	const [dropped, setDropped] = useState({});
	const [show, setShow] = useState(false);
	const [activeKey, setActiveKey] = useState<string | number>(0);

	const addTab = (compUnit: ast | null, name: string | null) =>
		setTabs(
			tabs.concat([
				{
					ast: compUnit,
					id: tabs.length,
					name,
				} as tab,
			]),
		);

	(window as any).electronAPI.addAST(({ compUnit, name }) =>
		addTab(compUnit, name),
	);

	(window as any).electronAPI.showModal(() => {
		setShow(true);
	});

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

	function handleCloseTab(id: number) {
		setTabs(tabs.filter((element) => element.id !== id));
		setActiveKey(0);
	}

	return (
		<Tab.Container
			onSelect={(eventKey) => setActiveKey(eventKey ? eventKey : 0)}
			activeKey={activeKey}
		>
			<Nav variant="tabs" className="flex-row">
				{tabs.map((element) => (
					<Nav.Item className="d-flex flex-row" key={element.id}>
						<Nav.Link eventKey={element.id.toString()}>
							{element.name}
							<CloseButton
								onClick={(event) => {
									event.stopPropagation();
									handleCloseTab(element.id);
								}}
							/>
						</Nav.Link>
					</Nav.Item>
				))}
			</Nav>
			{tabs.map((element) => (
				<Tab.Content key={element.id}>
					<Tab.Pane eventKey={element.id.toString()}>
						<DndContext
							modifiers={[snapCenterToCursor]}
							onDragStart={handleDragStart}
							onDragEnd={handleDragEnd}
						>
							<Bank compUnit={element.ast} activeId={activeId} />
							<Canvas>
								<DroppedContext.Provider value={[dropped, removeHandler]}>
									<ProofArea />
								</DroppedContext.Provider>
							</Canvas>
						</DndContext>
					</Tab.Pane>
				</Tab.Content>
			))}
			<Export show={show} onHide={() => setShow(false)} />
		</Tab.Container>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
