package com.rainmakeross.cryptocurrency.firstassignment;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {
    private UTXOPool currentPool;
    /**
     * Creates a public ledger whose current com.rainmakeross.cryptocurrency.firstassignment.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the com.rainmakeross.cryptocurrency.firstassignment.UTXOPool(com.rainmakeross.cryptocurrency.firstassignment.UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.currentPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        int ind = 0;
        int outputSum=0;
        int inputSum=0;
        Set<UTXO> outputHash = new HashSet<>();
        for(Transaction.Output output : tx.getOutputs()){
            UTXO utxo = new UTXO(output.address.getEncoded(), ind);
            // 1
            if(!currentPool.contains(new UTXO(output.address.getEncoded(), ind)))
                return false;
            // 3
            if(outputHash.contains(utxo))
                return false;
            outputHash.add(utxo);

            // 4
            if(output.value < 0)
                return false;

            outputSum += output.hashCode();
            ind++;
        }

        ind = 0;
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }

        //2
        for(Transaction.Input input : tx.getInputs()) {
            if(!Crypto.verifySignature(tx.getOutput(input.outputIndex).address,
                    tx.getRawTx(),input.signature))
                return false;
            inputSum += input.hashCode();
            ind++;
        }
        // 5
        if(outputSum > inputSum)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> validTransactions = new HashSet<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTransactions.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    currentPool.removeUTXO(utxo);
                }

                int i = 0;
                for (Transaction.Output out: tx.getOutputs()) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    currentPool.addUTXO(utxo, out);
                    i++;
                }
            }
        }

        Transaction[] returnArr = new Transaction[validTransactions.size()];
        return validTransactions.toArray(returnArr);
    }

}
