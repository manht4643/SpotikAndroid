import SwiftUI

// MARK: - Animated Mesh Gradient Background
// Uses iOS 18 MeshGradient when available, falls back to custom animated gradient.

struct MeshGradientBackground: View {
    @Environment(\.spotikPalette) var palette
    @State private var phase: CGFloat = 0
    @State private var timer: Timer? = nil

    var body: some View {
        TimelineView(.animation) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            if #available(iOS 18.0, *) {
                Mesh18Background(t: t, palette: palette)
            } else {
                LegacyMeshBackground(t: t, palette: palette)
            }
        }
        .ignoresSafeArea()
    }
}

// MARK: iOS 18 Native MeshGradient

@available(iOS 18.0, *)
private struct Mesh18Background: View {
    let t: Double
    let palette: SpotikPalette

    private func pts(_ t: Double) -> [SIMD2<Float>] {
        let noise = SimplexNoise.self
        var pts: [SIMD2<Float>] = []
        let grid = 4
        for row in 0..<grid {
            for col in 0..<grid {
                let bx = Float(col) / Float(grid - 1)
                let by = Float(row) / Float(grid - 1)
                let nx = Float(noise.noise(x: Double(col) * 0.7, y: Double(row) * 0.7, z: t * 0.18)) * 0.08
                let ny = Float(noise.noise(x: Double(col) * 0.7 + 5, y: Double(row) * 0.7 + 5, z: t * 0.18)) * 0.08
                pts.append(SIMD2<Float>(bx + nx, by + ny))
            }
        }
        return pts
    }

    private func colors(_ t: Double) -> [Color] {
        let base = palette.meshColors
        return base.enumerated().map { idx, c in
            let s = Float(SimplexNoise.noise(x: Double(idx) * 1.3, y: t * 0.12, z: 0))
            return c.opacity(Double(0.85 + s * 0.15))
        }
    }

    var body: some View {
        MeshGradient(
            width: 4, height: 4,
            points: pts(t),
            colors: colors(t)
        )
        .ignoresSafeArea()
    }
}

// MARK: Legacy (iOS 16/17) gradient background

private struct LegacyMeshBackground: View {
    let t: Double
    let palette: SpotikPalette

    var body: some View {
        GeometryReader { geo in
            Canvas { ctx, size in
                let w = size.width
                let h = size.height
                // Draw a multi-stop radial gradient that moves over time
                let x1 = w * (0.3 + 0.2 * sin(t * 0.4))
                let y1 = h * (0.25 + 0.15 * cos(t * 0.3))
                let x2 = w * (0.7 + 0.2 * cos(t * 0.35))
                let y2 = h * (0.7 + 0.15 * sin(t * 0.25))

                // Background
                ctx.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .color(palette.deepBg)
                )

                // First glow
                let r1 = Gradient(colors: [
                    palette.accent.opacity(0.35),
                    palette.accent.opacity(0)
                ])
                ctx.fill(
                    Path(ellipseIn: CGRect(x: x1 - w*0.5, y: y1 - h*0.5, width: w, height: h)),
                    with: .radialGradient(r1,
                        center: .init(x: x1, y: y1),
                        startRadius: 0,
                        endRadius: w * 0.5
                    )
                )

                // Second glow
                let r2 = Gradient(colors: [
                    palette.navGlow.opacity(0.25),
                    palette.navGlow.opacity(0)
                ])
                ctx.fill(
                    Path(ellipseIn: CGRect(x: x2 - w*0.4, y: y2 - h*0.4, width: w*0.8, height: h*0.8)),
                    with: .radialGradient(r2,
                        center: .init(x: x2, y: y2),
                        startRadius: 0,
                        endRadius: w * 0.4
                    )
                )
            }
        }
        .ignoresSafeArea()
    }
}

// MARK: - Simplex Noise (2D/3D)

enum SimplexNoise {
    private static let perm: [Int] = {
        var p = Array(0..<256)
        // deterministic shuffle
        for i in stride(from: 255, through: 1, by: -1) {
            let j = (i * 7 + 11) % (i + 1)
            p.swapAt(i, j)
        }
        return p + p
    }()

    static func noise(x: Double, y: Double, z: Double) -> Double {
        let F3 = 1.0 / 3.0
        let G3 = 1.0 / 6.0
        let s = (x + y + z) * F3
        let i = fastFloor(x + s)
        let j = fastFloor(y + s)
        let k = fastFloor(z + s)
        let t = Double(i + j + k) * G3
        let x0 = x - (Double(i) - t)
        let y0 = y - (Double(j) - t)
        let z0 = z - (Double(k) - t)
        var i1, j1, k1, i2, j2, k2: Int
        if x0 >= y0 {
            if y0 >= z0 { (i1,j1,k1,i2,j2,k2) = (1,0,0,1,1,0) }
            else if x0 >= z0 { (i1,j1,k1,i2,j2,k2) = (1,0,0,1,0,1) }
            else { (i1,j1,k1,i2,j2,k2) = (0,0,1,1,0,1) }
        } else {
            if y0 < z0 { (i1,j1,k1,i2,j2,k2) = (0,0,1,0,1,1) }
            else if x0 < z0 { (i1,j1,k1,i2,j2,k2) = (0,1,0,0,1,1) }
            else { (i1,j1,k1,i2,j2,k2) = (0,1,0,1,1,0) }
        }
        let x1 = x0 - Double(i1) + G3
        let y1 = y0 - Double(j1) + G3
        let z1 = z0 - Double(k1) + G3
        let x2 = x0 - Double(i2) + 2*G3
        let y2 = y0 - Double(j2) + 2*G3
        let z2 = z0 - Double(k2) + 2*G3
        let x3 = x0 - 1 + 3*G3
        let y3 = y0 - 1 + 3*G3
        let z3 = z0 - 1 + 3*G3
        let ii = i & 255; let jj = j & 255; let kk = k & 255
        let gi0 = perm[ii + perm[jj + perm[kk]]] % 12
        let gi1 = perm[ii+i1 + perm[jj+j1 + perm[kk+k1]]] % 12
        let gi2 = perm[ii+i2 + perm[jj+j2 + perm[kk+k2]]] % 12
        let gi3 = perm[ii+1 + perm[jj+1 + perm[kk+1]]] % 12
        var n0, n1, n2, n3: Double
        var t0 = 0.6 - x0*x0 - y0*y0 - z0*z0
        if t0 < 0 { n0 = 0 } else { t0 *= t0; n0 = t0*t0*dot3(gi0, x0, y0, z0) }
        var t1 = 0.6 - x1*x1 - y1*y1 - z1*z1
        if t1 < 0 { n1 = 0 } else { t1 *= t1; n1 = t1*t1*dot3(gi1, x1, y1, z1) }
        var t2 = 0.6 - x2*x2 - y2*y2 - z2*z2
        if t2 < 0 { n2 = 0 } else { t2 *= t2; n2 = t2*t2*dot3(gi2, x2, y2, z2) }
        var t3 = 0.6 - x3*x3 - y3*y3 - z3*z3
        if t3 < 0 { n3 = 0 } else { t3 *= t3; n3 = t3*t3*dot3(gi3, x3, y3, z3) }
        return 32.0 * (n0 + n1 + n2 + n3)
    }

    private static let grad3: [(Double,Double,Double)] = [
        (1,1,0),(-1,1,0),(1,-1,0),(-1,-1,0),
        (1,0,1),(-1,0,1),(1,0,-1),(-1,0,-1),
        (0,1,1),(0,-1,1),(0,1,-1),(0,-1,-1)
    ]

    private static func dot3(_ gi: Int, _ x: Double, _ y: Double, _ z: Double) -> Double {
        let g = grad3[gi]
        return g.0*x + g.1*y + g.2*z
    }

    private static func fastFloor(_ x: Double) -> Int {
        x > 0 ? Int(x) : Int(x) - 1
    }
}
