import React, { Component } from "react";

import { TransformWrapper, TransformComponent } from "react-zoom-pan-pinch";

const Canvas = () => {
	return (
		<div className="zoomable-canvas border border-5">
			<TransformWrapper>
				{({ zoomIn, zoomOut, resetTransform, ..._ }) => (
					<React.Fragment>
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
							<div className="canvas">Hello world</div>
						</TransformComponent>
					</React.Fragment>
				)}
			</TransformWrapper>
		</div>
	);
};

export default Canvas;
