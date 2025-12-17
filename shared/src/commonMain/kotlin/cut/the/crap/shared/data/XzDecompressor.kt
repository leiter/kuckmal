package cut.the.crap.shared.data

/**
 * Platform-specific XZ decompression.
 * JVM uses org.tukaani:xz, iOS uses LZMA via native compression.
 */
expect class XzDecompressor() {
    /**
     * Decompress XZ data from input bytes to output bytes.
     * @param inputData The XZ compressed data
     * @return The decompressed data
     * @throws Exception if decompression fails
     */
    fun decompress(inputData: ByteArray): ByteArray

    /**
     * Decompress XZ file to output file.
     * Streams data to avoid memory issues with large files.
     * @param inputPath Path to the XZ compressed file
     * @param outputPath Path for the decompressed output
     * @throws Exception if decompression fails
     */
    suspend fun decompressFile(inputPath: String, outputPath: String)
}
