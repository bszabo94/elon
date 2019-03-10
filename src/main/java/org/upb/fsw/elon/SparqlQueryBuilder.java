package org.upb.fsw.elon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.aksw.qa.annotation.spotter.ASpotter;
import org.aksw.qa.annotation.spotter.Spotlight;
import org.aksw.qa.commons.datastructure.Entity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;

public class SparqlQueryBuilder {

	private static String PROPERTY_QUERY_TEMPLATE = "select distinct ?property where { %s ?property [] . }";

	public ASpotter spotter;

	public static void main(String[] args) throws URISyntaxException, IOException {

		ASpotter spotter = new Spotlight();

		String qald9train = "";
//
//			for (Tree t : np) {
//				if (possibleEntity(t)) {
//					List<Entity> foundEntities = spotter.getEntities(treeToString(t, true)).get("en");
//					writer.write(treeToString(t, true) + "\n***********\n");
//					if (foundEntities == null || foundEntities.isEmpty())
//						continue;
//					for (Entity e : foundEntities) {
//						writer.write(e.getUris().get(0).getURI()+"\n");
//					}
//				}
//			}
////				writer.write("depth: " + t.depth() + "\n" + t.pennString() + "\n");
//		}
//
//		writer.close();

	}

	public SparqlQueryBuilder() {
		this.spotter = new Spotlight();
	}

	public String evalTree(Tree tree) {

		switch (tree.label().value()) {
		case "ROOT":
			return evalTree(tree.getChild(0));
		case "SBARQ":
			return evalSBARQ(tree);

		}

		return null;
	}

	public void traverseTree(Tree tree) {
		System.out.println("lvl 0");
		System.out.println(tree);

		System.out.println("lvl 1");
		List<Tree> children1 = tree.getChildrenAsList().get(0).getChildrenAsList();
		System.out.println(children1);
		for (Tree t : children1)
			System.out.println(t + "\n-------");

	}

	public String evalSBARQ(Tree sbarq) {
		return evalSQ(getChildbyLabel(sbarq, "SQ"));
	}

	public String evalSQ(Tree sq) {
//		String np = evalNP(getChildbyLabel(sq, "NP"));
//
//		String pp = evalPP(getChildbyLabel(sq, "PP"));
		return evalNP(getChildbyLabel(sq, "NP"));
	}

	public String evalPP(Tree pp) {
		return evalNP(getChildbyLabel(pp, "NP"));
	}

	public String evalNP(Tree np) {
		if (possibleEntity(np)) {
			List<Entity> foundEntities = this.spotter.getEntities(treeToString(np, true)).get("en");

			// TODO occurance for more than one possible entities
			Entity candidate = foundEntities.get(0);

			return candidate.getUris().get(0).getURI();

		} else if (hasChildWithLabel(np, "PP")) {
			String entityURI = evalPP(getChildbyLabel(np, "PP"));
			String propertyURI = findProperty(entityURI, treeToString(getChildbyLabel(np, "NP"), false));
			
			List<String> answer = QueryController.findPropertyofEntity("<" + propertyURI + ">", "<" + entityURI + ">");
			
			String answers = "";
			for(String a : answer)
				answers += a + "\n";
			
			return answers;
			
		} else {
			return null;
		}
	}

	public String findProperty(String entityURI, String property) {
		String properEntityURI = "<" + entityURI + ">";
		String queryString = String.format(PROPERTY_QUERY_TEMPLATE, properEntityURI);
		List<String> propertyCandidates = QueryController.doQueryAsList(queryString);

		sortRelations(propertyCandidates, property);

		if (!propertyCandidates.isEmpty())
			return propertyCandidates.get(0);
		else
			return null;
	}

	public void sortRelations(List<String> relations, String base) {
		relations.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				LevenshteinDistance dist = LevenshteinDistance.getDefaultInstance();
				Integer d1 = dist.apply(removeNS(o1).toLowerCase(), base.toLowerCase());
				Integer d2 = dist.apply(removeNS(o2).toLowerCase(), base.toLowerCase());

				return d1 - d2;
			}

		});
	}

	public void processQuestion(String question) {
		Sentence sentence = new Sentence(question);
		Tree tree = sentence.parse();

//		traverseTree(tree);

//		System.out.println(tree.pennString());
//		Sentence sentence2 = new Sentence("What's")

//		System.out.println("----");
//		System.out.println(treeToString(tree, false));
	}

	public boolean possibleEntity(Tree t) {
		if (t.depth() > 2)
			return false;

		for (Tree child : t.getChildrenAsList())
			if (child.label().value().equals("NNP") || child.label().value().equals("NNPS"))
				return true;

		return false;
	}

	public boolean hasChildWithLabel(Tree t, String label) {
		for (Tree child : t.getChildrenAsList())
			if (child.label().value().equals(label))
				return true;

		return false;
	}

	public Tree getChildbyLabel(Tree tree, String label) {
		for (Tree child : tree.getChildrenAsList())
			if (child.value().toUpperCase().equals(label.toUpperCase()))
				return child;

		return null;
	}

	public String removeNS(String fullname) {
		String reverse = new StringBuilder(fullname).reverse().toString();
		reverse = reverse.substring(0, reverse.indexOf('/'));

		return new StringBuilder(reverse).reverse().toString();
	}

	public Tree getNode(Tree tree, String value) {
		if (tree == null)
			return null;

		if (tree.label().value().equals(value))
			return tree;
		for (Tree child : tree.getChildrenAsList()) {
			Tree posNode = getNode(child, value);
			if (posNode != null)
				return posNode;
		}
		return null;
	}

	public List<Tree> getNodes(Tree tree, String value) {
		List<Tree> nodes = new ArrayList<Tree>();
		if (tree == null)
			return nodes;

		if (tree.label().value().equals(value))
			nodes.add(tree);

		for (Tree child : tree.getChildrenAsList()) {
			nodes.addAll(getNodes(child, value));
		}

		return nodes;
	}

	public List<Tree> getAllNodes(Tree tree, String value, boolean onlyLeafs) {
		List<Tree> nodes = new ArrayList<Tree>(), childNodes = new ArrayList<Tree>();

		for (Tree child : tree.getChildrenAsList()) {
			childNodes.addAll(getAllNodes(child, value, onlyLeafs));
		}

		if (tree.label().value().equals(value)) {
			if (childNodes.isEmpty() || !onlyLeafs)
				nodes.add(tree);
		}

		nodes.addAll(childNodes);

		return nodes;
	}

	public String treeToString(Tree tree, boolean enableDT) {
		StringBuilder sb = new StringBuilder();

		for (Tree child : tree.getChildrenAsList()) {
			if (child.isLeaf()) {
				if (enableDT)
					sb.append(child.value().toString() + " ");
				else if (!tree.label().value().equals("DT")) {
					sb.append(child.value().toString() + " ");
				}

			}

			else
				sb.append(treeToString(child, enableDT) + " ");
		}

		return sb.toString().trim();

	}

}
