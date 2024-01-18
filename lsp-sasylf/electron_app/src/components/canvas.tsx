import React, { useEffect, RefObject } from "react";
import {
	TransformWrapper,
	TransformComponent,
	useTransformInit,
	useTransformEffect,
} from "react-zoom-pan-pinch";

import ListGroup from "react-bootstrap/ListGroup";
import { input } from "../types";

const TransformComponentWrapper = (props: {
	children: any;
	index: number;
	canvasStates: any;
	setCanvasStates: any;
	setTransform: any;
}) => {
	useTransformInit((_) => {
		const { scale, x, y } = props.canvasStates[props.index];
		props.setTransform(x, y, scale, 0);
	});

	useTransformEffect(({ state, instance }) => {
		const newCanvasStates = [...props.canvasStates];
		newCanvasStates[props.index] = {
			scale: state.scale,
			x: state.positionX,
			y: state.positionY,
		};
		props.setCanvasStates(newCanvasStates);
	});
	return props.children;
};

function TheoremList(props: {
	proofRef: RefObject<HTMLDivElement>;
	zoomToElement: any;
}) {
	if (props.proofRef.current == null) return <></>;
	const rootNodes = props.proofRef.current.getElementsByClassName("root-node");
	const n = rootNodes.length;
	const conclusions: string[] = Array(rootNodes.length);
	for (let i = 0; i < n; ++i) {
		const proofNode = rootNodes[i];
		const containers = proofNode.getElementsByClassName("conclusion");
		const container = containers[containers.length - 1];
		const conclusion = container.getElementsByTagName("span")[0].textContent;
		conclusions[i] = conclusion;
	}
	return (
		<ListGroup className="theorem-list m-1">
			{Array.from(Array(n).keys()).map((i, ind) => (
				<ListGroup.Item
					key={ind}
					onClick={() => props.zoomToElement(rootNodes[i])}
				>
					{conclusions[i]}
				</ListGroup.Item>
			))}
		</ListGroup>
	);
}

const Canvas = (props: {
	children: any;
	index: number;
	canvasStates: any;
	setCanvasStates: any;
	proofRef: RefObject<HTMLDivElement>;
	inputs: input[];
}) => {
	return (
		<div className="zoomable-canvas border border-5">
			<TransformWrapper
				limitToBounds={false}
				panning={{ excluded: ["panning-excluded"] }}
			>
				{({ zoomIn, zoomOut, resetTransform, setTransform, zoomToElement }) => (
					<>
						<div className="tools">
							<button
								className="btn btn-primary btn-lg m-1"
								onClick={() => zoomIn()}
							>
								+
							</button>
							<button
								className="btn btn-primary btn-lg m-1"
								onClick={() => zoomOut()}
							>
								-
							</button>
							<button
								className="btn btn-primary btn-lg m-1"
								onClick={() => resetTransform()}
							>
								Reset
							</button>
						</div>
						<TheoremList
							zoomToElement={zoomToElement}
							proofRef={props.proofRef}
						/>
						<TransformComponentWrapper
							index={props.index}
							canvasStates={props.canvasStates}
							setTransform={setTransform}
							setCanvasStates={props.setCanvasStates}
						>
							<TransformComponent wrapperClass="tcomponent">
								<div className="canvas">{props.children}</div>
							</TransformComponent>
						</TransformComponentWrapper>
					</>
				)}
			</TransformWrapper>
		</div>
	);
};

export default Canvas;
