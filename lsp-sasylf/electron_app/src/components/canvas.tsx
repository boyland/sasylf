import React from "react";
import { TransformWrapper, TransformComponent } from "react-zoom-pan-pinch";

const Canvas = (props: { children: any }) => {
	return (
		<div className="zoomable-canvas border border-5">
			<TransformWrapper panning={{excluded : ["panning-excluded"]}}>
				{({ zoomIn, zoomOut, resetTransform }) => (
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
						<TransformComponent wrapperClass="tcomponent">
							<div className="canvas">{props.children}</div>
						</TransformComponent>
					</>
				)}
			</TransformWrapper>
		</div>
	);
};

export default Canvas;
