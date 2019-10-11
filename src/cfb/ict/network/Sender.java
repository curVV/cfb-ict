package cfb.ict.network;

import cfb.ict.Properties;
import cfb.ict.cryptography.Hash;
import cfb.ict.tangle.Tangle;
import cfb.ict.tangle.Transaction;
import cfb.ict.tangle.Vertex;
import cfb.ict.utilities.Converter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.SortedMap;

import static cfb.ict.network.Node.PACKET_SIZE_IN_BYTES;

public class Sender extends Thread {

    final Properties properties;

    final SortedMap<Neighbor, Neighbor> neighbors;
    final DatagramSocket socket;

    final Tangle tangle;

    Sender(final Properties properties,
           final SortedMap<Neighbor, Neighbor> neighbors, final DatagramSocket socket,
           final Tangle tangle) {

        this.properties = properties;

        this.neighbors = neighbors;
        this.socket = socket;

        this.tangle = tangle;
    }

    @Override
    public void run() {

        final DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE_IN_BYTES], PACKET_SIZE_IN_BYTES);
        final byte[] packetTrits = new byte[Converter.lengthInTrits(PACKET_SIZE_IN_BYTES)];

        while (!properties.shuttingDown) {

            boolean allQueuesEmpty = true;

            final long lowestAcceptableLatestActivityTime = System.currentTimeMillis() - properties.neighborCooldownDuration * 1000L;
            for (final Neighbor neighbor : neighbors.keySet()) {

                if (neighbor.latestActivityTime < lowestAcceptableLatestActivityTime) {

                    neighbor.clearQueue();

                } else {

                    try {

                        final Hash transactionHash = neighbor.pullFromQueue();
                        if (transactionHash != null) {

                            allQueuesEmpty = false;

                            final Vertex vertex = tangle.get(transactionHash);
                            if (vertex != null && vertex.transaction != null) {

                                vertex.transaction.dump(packetTrits, 0);

                                int firstNonZeroTritOffset;
                                for (firstNonZeroTritOffset = 0; firstNonZeroTritOffset < Transaction.LENGTH; firstNonZeroTritOffset++) {

                                    if (packetTrits[firstNonZeroTritOffset] != 0) {

                                        break;
                                    }
                                }
                                final int numberOfSkippedTrits = (firstNonZeroTritOffset / Hash.LENGTH) * Hash.LENGTH;
                                System.arraycopy(packetTrits, numberOfSkippedTrits, packetTrits, 0, Transaction.LENGTH - numberOfSkippedTrits);
                                tangle.copyTransactionToRequestHash(packetTrits, Transaction.LENGTH - numberOfSkippedTrits);

                                Converter.convertTritsToBytes(packetTrits, 0, (Transaction.LENGTH - numberOfSkippedTrits) + Hash.LENGTH, packet.getData(), packet.getOffset());
                                packet.setLength(Converter.sizeInBytes((Transaction.LENGTH - numberOfSkippedTrits) + Hash.LENGTH));

                                for (final Neighbor anotherNeighbor : neighbors.keySet()) {

                                    if (anotherNeighbor.latestActivityTime < lowestAcceptableLatestActivityTime) {

                                        break;
                                    }

                                    anotherNeighbor.send(socket, packet);
                                }
                            }
                        }

                    } catch (final Exception e) {

                        e.printStackTrace();
                    }
                }
            }

            if (allQueuesEmpty) {

                try {

                    Thread.sleep(1);

                } catch (final InterruptedException e) {
                }
            }
        }
    }
}
