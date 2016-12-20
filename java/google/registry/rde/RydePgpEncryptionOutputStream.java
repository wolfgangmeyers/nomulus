// Copyright 2016 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.rde;

import static org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags.AES_128;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import google.registry.config.ConfigModule.Config;
import google.registry.util.ImprovedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SecureRandom;
import javax.annotation.WillNotClose;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

/**
 * OpenPGP encryption service that wraps an {@link OutputStream}.
 *
 * <p>This uses 128-bit AES (Rijndael) as the symmetric encryption algorithm. This is the only key
 * strength ICANN allows. The other valid algorithms are TripleDES and CAST5 per RFC 4880. It's
 * probably for the best that we're not using AES-256 since it's been weakened over the years to
 * potentially being worse than AES-128.
 *
 * <p>The key for the symmetric algorithm is generated by a random number generator which SHOULD
 * come from {@code /dev/random} (see: {@link sun.security.provider.NativePRNG}) but Java doesn't
 * offer any guarantees that {@link SecureRandom} isn't pseudo-random.
 *
 * <p>The asymmetric algorithm is whatever one is associated with the {@link PGPPublicKey} object
 * you provide. That should be either RSA or DSA, per the ICANN escrow spec. The underlying
 * {@link PGPEncryptedDataGenerator} class uses PGP Cipher Feedback Mode to chain blocks. No
 * integrity packet is used.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4880">RFC 4880 (OpenPGP Message Format)</a>
 * @see <a href="http://en.wikipedia.org/wiki/Advanced_Encryption_Standard">AES (Wikipedia)</a>
 */
@AutoFactory(allowSubclasses = true)
public class RydePgpEncryptionOutputStream extends ImprovedOutputStream {

  /**
   * The symmetric encryption algorithm to use. Do not change this value without checking the
   * RFCs to make sure the encryption algorithm and strength combination is allowed.
   *
   * @see org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
   */
  private static final int CIPHER = AES_128;

  /**
   * This option adds an additional checksum to the OpenPGP message. From what I can tell, this is
   * meant to fix a bug that made a certain type of message tampering possible. GPG will actually
   * complain on the command line when decrypting a message without this feature.
   *
   * <p>However I'm reasonably certain that this is not required if you have a signature (and
   * remember to use it!) and the ICANN requirements document do not mention this. So we're going
   * to leave it out.
   */
  private static final boolean USE_INTEGRITY_PACKET = false;

  /**
   * The source of random bits. This should not be changed at Google because it uses dev random
   * in production, and the testing environment is configured to make this go fast and not drain
   * system entropy.
   *
   * @see SecureRandom#getInstance(String)
   */
  private static final String RANDOM_SOURCE = "NativePRNG";

  /**
   * Creates a new instance that encrypts data for the owner of {@code receiverKey}.
   *
   * @param os is the upstream {@link OutputStream} which is not closed by this object
   * @throws IllegalArgumentException if {@code publicKey} is invalid
   * @throws RuntimeException to rethrow {@link PGPException} and {@link IOException}
   */
  public RydePgpEncryptionOutputStream(
      @Provided @Config("rdeRydeBufferSize") Integer bufferSize,
      @WillNotClose OutputStream os,
      PGPPublicKey receiverKey) {
    super(createDelegate(bufferSize, os, receiverKey));
  }

  private static
      OutputStream createDelegate(int bufferSize, OutputStream os, PGPPublicKey receiverKey) {
    try {
      PGPEncryptedDataGenerator encryptor = new PGPEncryptedDataGenerator(
          new JcePGPDataEncryptorBuilder(CIPHER)
              .setWithIntegrityPacket(USE_INTEGRITY_PACKET)
              .setSecureRandom(new SecureRandom())
              .setProvider(PROVIDER_NAME));
      encryptor.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(receiverKey));
      return encryptor.open(os, new byte[bufferSize]);
    } catch (IOException | PGPException e) {
      throw new RuntimeException(e);
    }
  }
}
