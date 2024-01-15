import React, { useState } from "react";
import {
	TransformWrapper,
	TransformComponent,
	useTransformInit,
	useTransformEffect,
} from "react-zoom-pan-pinch";

const TransformComponentWrapper = (props: {
	children: any;
	index: number;
	canvasStates: any;
	setCanvasStates: any;
	setTransform: any;
}) => {
	useTransformInit(({ state, instance }) => {
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

const Canvas = (props: {
	children: any;
	index: number;
	canvasStates: any;
	setCanvasStates: any;
}) => {
	return (
		<div className="zoomable-canvas border border-5">
			<TransformWrapper
				limitToBounds={false}
				panning={{ excluded: ["panning-excluded"] }}
			>
				{({ zoomIn, zoomOut, resetTransform, setTransform }) => (
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
