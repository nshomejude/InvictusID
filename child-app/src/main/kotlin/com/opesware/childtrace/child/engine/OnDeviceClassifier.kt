package com.opesware.childtrace.child.engine

import com.opesware.childtrace.shared.engine.Classifier
import com.opesware.childtrace.shared.model.RiskCategory

/**
 * On-device ML classifier seam. The launch build ships the rules engine; this
 * is where a bundled TensorFlow Lite model plugs in without touching the rest
 * of the pipeline.
 *
 * Until a model is bundled this returns no scores, so it never adds a false
 * positive — the rules engine carries detection. When the model lands, load it
 * from assets here, run inference on [text], and return per-category
 * probabilities; [com.opesware.childtrace.shared.engine.ClassifierRule] turns
 * those into flags above its threshold.
 */
class OnDeviceClassifier : Classifier {
    override fun classify(text: String): Map<RiskCategory, Double> {
        // TODO(ml): load TFLite model from assets and run inference.
        return emptyMap()
    }
}
