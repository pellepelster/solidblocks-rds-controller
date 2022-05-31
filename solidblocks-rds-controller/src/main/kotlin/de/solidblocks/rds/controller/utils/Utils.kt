package de.solidblocks.rds.controller.utils

import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509ExtensionUtils
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.io.pem.PemObject
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.*

data class PrivateAndPublicKey(val privateKey: String, val publicKey: String)

class Utils {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    companion object {
        fun generateSshKey(name: String): PrivateAndPublicKey {

            val generator = Ed25519KeyPairGenerator()
            generator.init(Ed25519KeyGenerationParameters(SecureRandom()))

            val keyPair = generator.generateKeyPair()

            val publicKey = "ssh-ed25519 " + Base64.getEncoder()
                .encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(keyPair.public))

            val content = OpenSSHPrivateKeyUtil.encodePrivateKey(keyPair.private)
            val privateKey = PemObject("OPENSSH PRIVATE KEY", content)

            return PrivateAndPublicKey(privateKey.toPemString(), publicKey)
        }

        fun generateX509Certificate(): PrivateAndPublicKey {

            val keyPairGenerator = KeyPairGenerator.getInstance("Ed25519", "BC")
            keyPairGenerator.initialize(EdDSAParameterSpec(EdDSAParameterSpec.Ed25519), SecureRandom())

            val ed25519Keypair = keyPairGenerator.generateKeyPair()

            val certificate = generateCertificate(ed25519Keypair, "Ed25519", "localhost", 7)
            val privateKey = PemObject("EC PRIVATE KEY", ed25519Keypair.private.encoded)

            return PrivateAndPublicKey(privateKey.toPemString(), certificate.toPemString())
        }

        private fun PemObject.toPemString() = StringWriter().use { stringWriter ->
            JcaPEMWriter(stringWriter).use { pemWriter ->
                pemWriter.writeObject(this)
            }

            stringWriter.toString()
        }

        private fun X509Certificate.toPemString() = StringWriter().use { stringWriter ->
            JcaPEMWriter(stringWriter).use { pemWriter ->
                pemWriter.writeObject(this)
            }

            stringWriter.toString()
        }

        private fun generateCertificate(
            keyPair: KeyPair,
            hashAlgorithm: String,
            cn: String,
            days: Long
        ): X509Certificate {

            val now: Instant = Instant.now()

            val notBefore = Date.from(now)
            val notAfter = Date.from(now.plus(Duration.ofDays(days)))

            val contentSigner = JcaContentSignerBuilder(hashAlgorithm).build(keyPair.private)
            val x500Name = X500Name("CN=$cn")

            val certificateBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                x500Name, BigInteger.valueOf(now.toEpochMilli()), notBefore, notAfter, x500Name, keyPair.public
            ).addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(keyPair.public))
                .addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(keyPair.public))
                .addExtension(Extension.basicConstraints, true, BasicConstraints(false))

            return JcaX509CertificateConverter().setProvider(BouncyCastleProvider())
                .getCertificate(certificateBuilder.build(contentSigner))
        }

        private fun createSubjectKeyId(publicKey: PublicKey): SubjectKeyIdentifier {
            val publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)

            val digestCalculatorProvider =
                BcDigestCalculatorProvider().get(AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1))
            return X509ExtensionUtils(digestCalculatorProvider).createSubjectKeyIdentifier(publicKeyInfo)
        }

        private fun createAuthorityKeyId(publicKey: PublicKey): AuthorityKeyIdentifier {
            val publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)
            val digestCalculatorProvider =
                BcDigestCalculatorProvider().get(AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1))

            return X509ExtensionUtils(digestCalculatorProvider).createAuthorityKeyIdentifier(publicKeyInfo)
        }
    }
}
