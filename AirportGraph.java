import fs from 'fs';
import csv from 'csv-parser';

class Pair {
    constructor(key, dist) {
        this.key = key;
        this.dist = dist;
    }
}

class AirportGraph {
    constructor() {
        this.adjacencyList = new Map();
        this.routeDistances = new Map();
    }

    addRoute(source, destination, distance) {
        if (!this.adjacencyList.has(source)) {
            this.adjacencyList.set(source, new Set());
        }
        this.adjacencyList.get(source).add(destination);
        this.routeDistances.set(`${source}-${destination}`, distance);
    }

    printAdjacencyList() {
        this.adjacencyList.forEach((value, key) => {
            console.log(`${key} -> ${Array.from(value).join(', ')}`);
        });
        console.log(`Number of nodes are ${this.adjacencyList.size}`);
    }

    shortestPathFinder(src, dest) {
        const dist = new Map();
        const prev = new Map();
        const pQueue = new MinPriorityQueue({ priority: (pair) => pair.dist });

        this.adjacencyList.forEach((_, key) => {
            dist.set(key, Infinity);
            prev.set(key, null);
        });

        dist.set(src, 0);
        pQueue.enqueue(new Pair(src, 0));

        while (!pQueue.isEmpty()) {
            const { element: { key: curNode, dist: curDist } } = pQueue.dequeue();

            if (!this.adjacencyList.has(curNode)) continue;

            for (const nextNode of this.adjacencyList.get(curNode)) {
                const routeKey = `${curNode}-${nextNode}`;
                const nextDistance = this.routeDistances.get(routeKey) || Infinity;

                if (curDist + nextDistance < dist.get(nextNode)) {
                    dist.set(nextNode, curDist + nextDistance);
                    prev.set(nextNode, curNode);
                    pQueue.enqueue(new Pair(nextNode, curDist + nextDistance));
                }
            }
        }

        if (dist.get(dest) === Infinity) {
            console.log(`No route found from ${src} to ${dest}`);
            return;
        }

        console.log(`The shortest distance from ${src} to ${dest} is: ${dist.get(dest)}`);

        const path = [];
        for (let at = dest; at != null; at = prev.get(at)) {
            path.push(at);
        }
        path.reverse();

        console.log(`The route taken is: ${path.join(' -> ')}`);
    }

    static buildFromFile(filePath) {
        const graph = new AirportGraph();
        return new Promise((resolve, reject) => {
            fs.createReadStream(filePath)
                .pipe(csv())
                .on('data', (row) => {
                    const source = row.source.trim();
                    const destination = row.dest.trim();
                    const distance = parseFloat(row.distance.trim());
                    graph.addRoute(source, destination, distance);
                })
                .on('end', () => {
                    console.log('CSV file successfully processed');
                    resolve(graph);
                })
                .on('error', (error) => {
                    reject(error);
                });
        });
    }
}

function userInterface(graph) {
    const readline = require('readline').createInterface({
        input: process.stdin,
        output: process.stdout
    });

    const promptUser = () => {
        console.log("Choose from the following options:");
        console.log("Enter 1 to print adjacency List");
        console.log("Enter 2 to print Itinerary from src to dest");
        console.log("Enter 3 to exit");
        readline.question("Enter your Input: ", (userInput) => {
            switch (parseInt(userInput)) {
                case 1:
                    graph.printAdjacencyList();
                    promptUser();
                    break;
                case 2:
                    readline.question("Please enter src: ", (srcInput) => {
                        readline.question("Please enter dest: ", (destInput) => {
                            graph.shortestPathFinder(srcInput, destInput);
                            promptUser();
                        });
                    });
                    break;
                case 3:
                    console.log("Exiting the system, Sayonara....");
                    readline.close();
                    break;
                default:
                    console.log("Invalid input, please try again.");
                    promptUser();
            }
        });
    };

    promptUser();
}

(async () => {
    try {
        const filePath = 'routes.csv';
        const graph = await AirportGraph.buildFromFile(filePath);
        userInterface(graph);
    } catch (error) {
        console.error(error);
    }
})();
