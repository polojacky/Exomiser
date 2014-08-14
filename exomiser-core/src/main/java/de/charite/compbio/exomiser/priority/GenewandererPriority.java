package de.charite.compbio.exomiser.priority;

import de.charite.compbio.exomiser.exception.ExomizerException;
import de.charite.compbio.exomiser.exception.ExomizerInitializationException;
import de.charite.compbio.exomiser.exome.Gene;
import de.charite.compbio.exomiser.priority.util.DataMatrix;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.jblas.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter genes according to the random walk proximity in the protein-protein
 * interaction network.
 * <P>
 * The files required for the constructor of this filter should be downloaded
 * from:
 * http://compbio.charite.de/hudson/job/randomWalkMatrix/lastSuccessfulBuild/artifact/
 * <P>
 * This class coordinates random walk analysis as described in the paper
 * <a hred="http://www.ncbi.nlm.nih.gov/pubmed/18371930">
 * Walking the interactome for prioritization of candidate disease genes</a>.
 *
 * @see <a
 * href="http://compbio.charite.de/hudson/job/randomWalkMatrix/">RandomWalk
 * Hudson page</a>
 * @author Sebastian Koehler
 * @version 0.09 (3 November, 2013)
 */
public class GenewandererPriority implements Priority {

    private static final Logger logger = LoggerFactory.getLogger(GenewandererPriority.class);

    private final PriorityType GENEWANDERER_PRIORITY = PriorityType.GENEWANDERER_PRIORITY;

    /**
     * A list of error-messages
     */
    private List<String> error_record = new ArrayList<>();

    /**
     * A list of messages that can be used to create a display in a HTML page or
     * elsewhere.
     */
    private List<String> messages = new ArrayList<>();

    /**
     * Number of variants considered by this filter
     */
    private int n_before = 0;
    /**
     * Number of variants after applying this filter.
     */
    private int n_after = 0;

    /**
     * The random walk matrix object
     */
    private DataMatrix randomWalkMatrix;

    /**
     * List of the Entrez Gene IDs corresponding to the disease gene family that
     * will be used to prioritize the genes with variants in the exome.
     */
    private List<Integer> seedGenes;

    /**
     * This is the matrix of similarities between the seeed genes and all genes
     * in the network, i.e., p<sub>infinity</sub>.
     */
    private DoubleMatrix combinedProximityVector;

    /**
     * Create a new instance of the {@link GenewandererPriority}.
     *
     * Assumes the list of seed genes (Entrez gene IDs) has been set!! This
     * happens with the method {@link #setParameters}.
     *
     * @param randomWalkMatrixFileZip The zipped(!) RandomWalk matrix file.
     * @param randomWalkGeneId2IndexFileZip The zipped(!) file with the mapping
     * between Entrez-Ids and Matrix-Indices.
     * @throws ExomizerInitializationException
     * @see <a
     * href="http://compbio.charite.de/hudson/job/randomWalkMatrix/">Uberpheno
     * Hudson page</a>
     */
    public GenewandererPriority(String randomWalkMatrixFileZip, String randomWalkGeneId2IndexFileZip) {

        if (randomWalkMatrix == null) {
            try {
                randomWalkMatrix = new DataMatrix(randomWalkMatrixFileZip, randomWalkGeneId2IndexFileZip, true);
            } catch (Exception e) {
                /* This exception is thrown if the files for the random walk cannot be found. */
                logger.error("Unable to initialize the random walk matrix", e);
            }
        }
    }

    /**
     * 
     * @param randomWalkMatrix
     * @param entrezSeedGenes 
     */
    public GenewandererPriority(DataMatrix randomWalkMatrix, List<Integer> entrezSeedGenes) {
        
        this.randomWalkMatrix = randomWalkMatrix;
        seedGenes = new ArrayList<>();
        addMatchedGenesToSeedGeneList(entrezSeedGenes);
        computeDistanceAllNodesFromStartNodes();
    }

    /**
     * Adds the Entrez ids in the list provided to the seedGenes if it is contained
     * in the DataMatrix.
     * 
     * @param entrezSeedGenes 
     */
    private void addMatchedGenesToSeedGeneList(List<Integer> entrezSeedGenes) {
        for (Integer entrezId : entrezSeedGenes) {
            
            if (randomWalkMatrix.getObjectid2idx().containsKey(entrezId)) {
                seedGenes.add(entrezId);
            } else {
                logger.warn("Cannot use entrez-id {} as seed gene as it is not present in the DataMatrix provided.", entrezId);
            }
        }

        if (this.seedGenes.isEmpty()) {
            logger.error("Could not find any of the given genes in random-walk matrix. You gave: {}", entrezSeedGenes);
        }
    }

    /**
     * @see exomizer.priority.Priority#getPriorityName()
     */
    @Override
    public String getPriorityName() {
        return "GeneWanderer";
    }

    /**
     * Flag to output results of filtering against Genewanderer.
     */
    @Override
    public PriorityType getPriorityType() {
        return PriorityType.GENEWANDERER_PRIORITY;
    }

    /**
     * Compute the distance of all genes in the Random Walk matrix to the set of
     * seed genes given by the user.
     */
    private void computeDistanceAllNodesFromStartNodes() {
        boolean first = true;
        DoubleMatrix combinedProximityVector = null;
        for (Integer seedGeneEntrezId : seedGenes) {
            //shouldn't happed as we've already thrown this in the constructor
//	    if (this.randomWalkMatrix == null) {
//		String e = "[GeneWanderer.java] Error: randomWalkMatrix is null";
//		throw new ExomizerInitializationException(e);
//	    }
//	    if (this.randomWalkMatrix.objectid2idx == null) {
//		String e = "[GeneWanderer.java] Error: randomWalkMatrix.object2idx is null";
//		throw new ExomizerInitializationException(e);
//	    }
            if (!randomWalkMatrix.getObjectid2idx().containsKey(seedGeneEntrezId)) {
                /* Note that the RW matrix does not have an entry for every
                 Entrez Gene. If the gene is not contained in the matrix, we
                 skip it. The gene will be given a (low) default score in 
                 Genewanderer Relevance.
                 */
                continue;
            }
            int indexOfGene = randomWalkMatrix.getObjectid2idx().get(seedGeneEntrezId);

            //    means the column we need, which has the distances of ALL genes to the current gene
            DoubleMatrix column = randomWalkMatrix.getData().getColumn(indexOfGene);

            // for the first column/known gene we have to init the resulting vector
            if (first) {
                combinedProximityVector = column;
                first = false;
            } else {
                combinedProximityVector = combinedProximityVector.add(column);
            }
        }
        /* p_{\infty} */
        this.combinedProximityVector = combinedProximityVector;
    }

    /**
     * @return list of messages representing process, result, and if any, errors
     * of score filtering.
     */
    public List<String> getMessages() {
        for (String s : error_record) {
            messages.add("Error: " + s);
        }
        return messages;
    }

    /**
     * Prioritize a list of candidate {@link exomizer.exome.Gene Gene} objects
     * (the candidate genes have rare, potentially pathogenic variants).
     * <P>
     * You have to call {@link #setParameters} before running this function.
     *
     * @param geneList List of candidate genes.
     * @see exomizer.filter.Filter#filter_list_of_variants(java.util.ArrayList)
     */
    @Override
    public void prioritizeGenes(List<Gene> geneList) {
        if (seedGenes == null || seedGenes.isEmpty()) {
            throw new RuntimeException("Please specify a valid list of known genes!");
        }

        int PPIdataAvailable = 0;
        int totalGenes = geneList.size();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (Gene gene : geneList) {
            GenewandererRelevanceScore relScore = null;;
            if (randomWalkMatrix.getObjectid2idx().containsKey(gene.getEntrezGeneID())) {
                double val = computeSimStartNodesToNode(gene);
                if (val > max) {
                    max = val;
                }
                if (val < min) {
                    min = val;
                }
                relScore = new GenewandererRelevanceScore(val);
                ++PPIdataAvailable;
            } else {
                relScore = GenewandererRelevanceScore.noPPIDataScore();
            }
            gene.addRelevanceScore(relScore, GENEWANDERER_PRIORITY);
        }

//        float factor = 1f / (float) max;
//        float factorMaxPossible = 1f / (float) combinedProximityVector.max();
//
//        for (Gene gene : geneList) {
//            float scr = gene.getRelevanceScore(GENEWANDERER_PRIORITY);
//            float newscore = factor * (scr - (float) min);
//            gene.resetRelevanceScore(GENEWANDERER_PRIORITY, newscore);
//            newscore = factorMaxPossible * (scr - (float) min);
//            gene.resetRelevanceScore(GENEWANDERER_PRIORITY, newscore);
//        }

        String s = String.format("Protein-Protein Interaction Data was available for %d of %d genes (%.1f%%)",
                PPIdataAvailable, totalGenes, 100f * ((float) PPIdataAvailable / (float) totalGenes));
        this.n_before = totalGenes;
        this.n_after = PPIdataAvailable;
        this.messages.add(s);
        StringBuilder sb = new StringBuilder();
        sb.append("Seed genes:");
        for (Integer seed : seedGenes) {
            sb.append(seed + "&nbsp;");
        }
        this.messages.add(sb.toString());
    }

    /**
     * This causes a summary of RW prioritization to appear in the HTML output
     * of the exomizer
     */
    public boolean displayInHTML() {
        return true;
    }

    /**
     * @return HTML code for displaying the HTML output of the Exomizer.
     */
    public String getHTMLCode() {
        if (messages == null) {
            return "Error initializing Random Walk matrix";
        } else if (messages.size() == 1) {
            return String.format("<ul><li>%s</li></ul>", messages.get(0));
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("<ul>\n");
            for (String m : messages) {
                sb.append(String.format("<li>%s</li>\n", m));
            }
            sb.append("</ul>\n");
            return sb.toString();
        }
    }

    /**
     * Get number of variants before filter was applied.
     */
    public int getBefore() {
        return this.n_before;
    }

    /**
     * Get number of variants after filter was applied.
     */
    public int getAfter() {
        return this.n_after;
    }

    /**
     * Set parameters of the GeneWanderer prioritizer. We expect to get a string
     * of entrez gene ids separated by a comma, e.g.,
     * <pre>
     * 123,2123,34445,12321
     * </pre> This String is parsed and put into the class variable
     * {@link #seedGenes}.
     *
     * @param par A String with entrez gene ids separated by a comma.
     * @Deprecated Use the constructor to set the Entrez gene ids. 
     */
    @Override
    @Deprecated
    public void setParameters(String par) {
        String A[] = par.split(",");
        List<Integer> parsedIntegers = new ArrayList<>();
        for (String a : A) {
            try {
                Integer entrezIdInt = Integer.parseInt(a.trim());
                parsedIntegers.add(entrezIdInt);
            } catch (NumberFormatException e) {
                logger.error("Error encountered while setting up random walk analysis due to malformed Entrez Gene id: {}", a, e);
            }
        }
        
        addMatchedGenesToSeedGeneList(parsedIntegers);
        computeDistanceAllNodesFromStartNodes();
    }
    
    /**
     * This function retrieves the random walk similarity score for the gene
     *
     * @param nodeToCompute Gene for which the RW score is to bee retrieved
     */
    private double computeSimStartNodesToNode(Gene nodeToCompute) {
        int idx = randomWalkMatrix.getObjectid2idx().get(nodeToCompute.getEntrezGeneID());
        double val = combinedProximityVector.get(idx, 0);
        return val;
    }

    /**
     * This class does not need a database connection, this function only there
     * to satisfy the interface.
     *
     * @param connection An SQL (postgres) connection that was initialized
     * elsewhere.
     */
    @Override
    public void setDatabaseConnection(Connection connection) { /* no-op */ }

}