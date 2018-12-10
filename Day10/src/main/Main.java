package main;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import main.Tokenizer.Token;

public class Main {
	private static String loadFile(String path) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(new File(path)));
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line).append("\n");
			}
			reader.close();
			return result.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		String input = loadFile("src/input.txt");
		Tokenizer.addDefinition(1, "-?\\d+", 1);
		List<Token> tokens = new ArrayList<Token>();
		Tokenizer.run(input, tokens);

		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < tokens.size(); i += 4) {
			Point point = new Point();
			point.x = Integer.parseInt(tokens.get(i + 0).val);
			point.y = Integer.parseInt(tokens.get(i + 1).val);
			point.dx = Integer.parseInt(tokens.get(i + 2).val);
			point.dy = Integer.parseInt(tokens.get(i + 3).val);
			points.add(point);
		}

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(1280, 720));
		frame.setVisible(true);
	}

	static class Point {
		int x, y;
		int dx, dy;
	}
}
