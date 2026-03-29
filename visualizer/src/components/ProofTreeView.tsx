import React, { useEffect, useRef } from 'react';
import * as d3 from 'd3';
import { ProofTree, DerivationStep, TreeNode } from '../types/ProofTypes';

interface ProofTreeViewProps {
    proofTree: ProofTree;
    onStepSelect: (step: DerivationStep) => void;
    selectedStep: DerivationStep | null;
}

const ProofTreeView: React.FC<ProofTreeViewProps> = ({ proofTree, onStepSelect, selectedStep }) => {
    const svgRef = useRef<SVGSVGElement>(null);

    useEffect(() => {
        if (!svgRef.current || !proofTree.roots.length) return;

        // Clear previous content
        d3.select(svgRef.current).selectAll('*').remove();

        const width = 800;
        const height = 600;
        const nodeRadius = 40;

        // Create SVG
        const svg = d3.select(svgRef.current)
            .attr('width', width)
            .attr('height', height)
            .attr('viewBox', [0, 0, width, height]);

        // Create container for zoom
        const g = svg.append('g');

        // Convert to D3 hierarchy
        const flattenTree = (step: DerivationStep, parentId = ''): TreeNode => {
            const id = parentId ? `${parentId}-${step.name}` : step.name;
            return {
                ...step,
                id,
                children: step.children.map((child, idx) => flattenTree(child, id))
            };
        };

        const root = d3.hierarchy<TreeNode>(flattenTree(proofTree.roots[0]));

        // Create tree layout
        const treeLayout = d3.tree<TreeNode>()
            .size([width - 100, height - 100]);

        treeLayout(root);

        // Draw links
        g.selectAll('.link')
            .data(root.links())
            .enter()
            .append('path')
            .attr('class', 'link')
            .attr('d', d3.linkVertical<any, any>()
                .x(d => d.x + 50)
                .y(d => d.y + 50))
            .attr('fill', 'none')
            .attr('stroke', '#8b5cf6')
            .attr('stroke-width', 2)
            .attr('opacity', 0.6);

        // Draw nodes
        const nodes = g.selectAll('.node')
            .data(root.descendants())
            .enter()
            .append('g')
            .attr('class', 'node')
            .attr('transform', d => `translate(${d.x! + 50},${d.y! + 50})`)
            .style('cursor', 'pointer')
            .on('click', (event, d) => {
                onStepSelect(d.data);
            });

        // Node circles
        nodes.append('circle')
            .attr('r', nodeRadius)
            .attr('fill', d => {
                if (selectedStep && d.data.name === selectedStep.name) {
                    return '#a78bfa';
                }
                return d.data.completed ? '#8b5cf6' : '#6366f1';
            })
            .attr('stroke', '#fff')
            .attr('stroke-width', 2)
            .style('filter', 'drop-shadow(0 4px 6px rgba(0, 0, 0, 0.3))');

        // Node labels
        nodes.append('text')
            .attr('dy', '.35em')
            .attr('text-anchor', 'middle')
            .attr('fill', '#fff')
            .attr('font-size', '14px')
            .attr('font-weight', 'bold')
            .text(d => d.data.name);

        // Add zoom behavior
        const zoom = d3.zoom<SVGSVGElement, unknown>()
            .scaleExtent([0.5, 3])
            .on('zoom', (event) => {
                g.attr('transform', event.transform);
            });

        svg.call(zoom as any);

    }, [proofTree, selectedStep, onStepSelect]);

    return (
        <div className="w-full h-[500px] bg-slate-900/50 rounded-lg overflow-hidden">
            <svg ref={svgRef} className="w-full h-full" />
        </div>
    );
};

export default ProofTreeView;
