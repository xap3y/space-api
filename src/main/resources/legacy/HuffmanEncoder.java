package me.xap3y.space.util;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class HuffmanEncoder {

    private static class Node implements Comparable<Node> {
        char ch;
        int freq;
        Node left, right;

        Node(char ch, int freq) {
            this.ch = ch;
            this.freq = freq;
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.freq = left.freq + right.freq;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.freq, other.freq);
        }
    }

    private static void buildCodeMap(Node node, String code, Map<Character, String> map) {
        if (node.isLeaf()) {
            map.put(node.ch, !code.isEmpty() ? code : "0"); // handle edge case
        } else {
            buildCodeMap(node.left, code + '0', map);
            buildCodeMap(node.right, code + '1', map);
        }
    }

    private static Map<Character, String> buildCodeMap(Node root) {
        Map<Character, String> map = new HashMap<>();
        buildCodeMap(root, "", map);
        return map;
    }

    private static void serializeTree(Node node, DataOutputStream out) throws IOException {
        if (node.isLeaf()) {
            out.writeBoolean(true);
            out.writeChar(node.ch);
        } else {
            out.writeBoolean(false);
            serializeTree(node.left, out);
            serializeTree(node.right, out);
        }
    }

    private static Node deserializeTree(DataInputStream in) throws IOException {
        boolean isLeaf = in.readBoolean();
        if (isLeaf) {
            return new Node(in.readChar(), 0);
        } else {
            Node left = deserializeTree(in);
            Node right = deserializeTree(in);
            return new Node(left, right);
        }
    }

    public byte[] encode(String input) {
        if (input == null || input.isEmpty()) return new byte[0];

        // Count frequencies
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : input.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        // Build priority queue
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (var entry : freqMap.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }

        if (pq.size() == 1) pq.add(new Node('\0', 1)); // Edge case

        // Build Huffman tree
        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            assert right != null;
            pq.add(new Node(left, right));
        }

        Node root = pq.poll();

        // Build code map
        Map<Character, String> codeMap = buildCodeMap(root);

        // Encode bit sequence
        BitSet bitSet = new BitSet();
        int bitIndex = 0;
        for (char c : input.toCharArray()) {
            String code = codeMap.get(c);
            for (char b : code.toCharArray()) {
                if (b == '1') {
                    bitSet.set(bitIndex);
                }
                bitIndex++;
            }
        }

        // Serialize tree and bit data into byte[]
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            // Header: compression flag (true) and input length (for safety)
            out.writeBoolean(true);
            out.writeInt(input.length());

            // Serialize tree
            assert root != null;
            serializeTree(root, out);

            // Bit length and bit data
            out.writeInt(bitIndex); // total number of bits
            byte[] bitBytes = bitSet.toByteArray();
            out.writeInt(bitBytes.length);
            out.write(bitBytes);

            byte[] result = baos.toByteArray();

            // If not smaller than original, fallback to uncompressed
            if (result.length >= input.length() * 2) {
                return storeRaw(input);
            }

            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] storeRaw(String input) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            out.writeBoolean(false); // uncompressed
            out.writeUTF(input);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) return "";

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
             DataInputStream in = new DataInputStream(bais)) {

            boolean isCompressed = in.readBoolean();

            if (!isCompressed) {
                return in.readUTF();
            }

            int originalLength = in.readInt();

            Node root = deserializeTree(in);

            int bitCount = in.readInt();
            int byteCount = in.readInt();
            byte[] bitBytes = new byte[byteCount];
            in.readFully(bitBytes);

            BitSet bits = BitSet.valueOf(bitBytes);

            StringBuilder decoded = new StringBuilder();
            Node current = root;
            for (int i = 0; i < bitCount; i++) {
                current = bits.get(i) ? current.right : current.left;
                if (current.isLeaf()) {
                    decoded.append(current.ch);
                    current = root;
                }
            }

            return decoded.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
